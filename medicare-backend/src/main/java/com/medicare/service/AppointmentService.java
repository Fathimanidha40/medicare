package com.medicare.service;

import com.medicare.dto.AppointmentRequest;
import com.medicare.entity.Appointment;
import com.medicare.entity.Doctor;
import com.medicare.entity.User;
import com.medicare.exception.BadRequestException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.AppointmentRepository;
import com.medicare.repository.DoctorRepository;
import com.medicare.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
    }

    public List<Appointment> getUserAppointments(String userEmail) {
        User user = getUserByEmail(userEmail);
        return appointmentRepository.findByUserIdOrderByAppointmentDateTimeDesc(user.getId());
    }

    public List<Appointment> getUpcomingAppointments(String userEmail) {
        User user = getUserByEmail(userEmail);
        return appointmentRepository.findByUserIdAndAppointmentDateTimeAfterOrderByAppointmentDateTimeAsc(user.getId(), LocalDateTime.now());
    }

    @Transactional
    public Appointment bookAppointment(String userEmail, AppointmentRequest req) {
        User user = getUserByEmail(userEmail);
        Doctor doctor = resolveDoctor(req);

        // Prevent double-booking
        boolean isBooked = appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatusNotCancelled(
                doctor.getId(), req.getAppointmentDateTime()
        );
        if (isBooked) {
            throw new BadRequestException("The selected slot with " + doctor.getName() + " is already booked. Please choose another time.");
        }

        Appointment appointment = new Appointment(
                user,
                doctor,
                req.getAppointmentDateTime(),
                req.getAppointmentType() != null ? req.getAppointmentType() : "IN_PERSON",
                req.getReason(),
                req.getNotes()
        );

        return appointmentRepository.save(appointment);
    }

    private Doctor resolveDoctor(AppointmentRequest req) {
        if (req.getDoctorId() != null) {
            return doctorRepository.findById(req.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + req.getDoctorId()));
        }
        String docName = (req.getDoctorName() != null && !req.getDoctorName().isBlank())
                ? req.getDoctorName().trim()
                : "General Healthcare Provider";
        String clinic = (req.getClinicName() != null && !req.getClinicName().isBlank())
                ? req.getClinicName().trim()
                : "MediCare Clinic";

        return doctorRepository.findFirstByNameIgnoreCase(docName)
                .orElseGet(() -> {
                    Doctor newDoc = new Doctor();
                    newDoc.setName(docName);
                    newDoc.setSpecialty("General Practitioner");
                    newDoc.setQualification("Licensed Physician");
                    newDoc.setClinicName(clinic);
                    newDoc.setAddress("Consultation Center");
                    newDoc.setContactNumber("+1-555-MED-CARE");
                    newDoc.setActive(true);
                    return doctorRepository.save(newDoc);
                });
    }

    @Transactional
    public Appointment updateAppointment(Long appointmentId, String userEmail, AppointmentRequest req) {
        User user = getUserByEmail(userEmail);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if (!appointment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to edit this appointment");
        }

        if (req.getAppointmentDateTime() != null) {
            appointment.setAppointmentDateTime(req.getAppointmentDateTime());
        }
        if (req.getAppointmentType() != null) {
            appointment.setAppointmentType(req.getAppointmentType());
        }
        if (req.getReason() != null) {
            appointment.setReason(req.getReason());
        }
        if (req.getNotes() != null) {
            appointment.setNotes(req.getNotes());
        }
        if (req.getDoctorId() != null || (req.getDoctorName() != null && !req.getDoctorName().isBlank())) {
            Doctor doc = resolveDoctor(req);
            appointment.setDoctor(doc);
        }

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public void deleteAppointment(Long appointmentId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if (!appointment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to delete this appointment");
        }

        appointmentRepository.delete(appointment);
    }

    @Transactional
    public Appointment rescheduleAppointment(Long appointmentId, String userEmail, LocalDateTime newDateTime) {
        User user = getUserByEmail(userEmail);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if (!appointment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to reschedule this appointment");
        }

        if (newDateTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("New appointment time must be in the future");
        }

        boolean isBooked = appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatusNotCancelledExcludingId(
                appointment.getDoctor().getId(), newDateTime, appointmentId
        );
        if (isBooked) {
            throw new BadRequestException("The selected slot is already booked. Please pick an alternative time.");
        }

        appointment.setAppointmentDateTime(newDateTime);
        appointment.setStatus("RESCHEDULED");
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment cancelAppointment(Long appointmentId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if (!appointment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to cancel this appointment");
        }

        appointment.setStatus("CANCELLED");
        return appointmentRepository.save(appointment);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
