package com.medicare.service;

import com.medicare.entity.Doctor;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.AppointmentRepository;
import com.medicare.repository.DoctorRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorService(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public List<Doctor> getAllActiveDoctors() {
        List<Doctor> doctors = doctorRepository.findByActiveTrue();
        if (doctors.isEmpty()) {
            initSampleDoctors();
            doctors = doctorRepository.findByActiveTrue();
        }
        return doctors;
    }

    public Doctor getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));
    }

    public List<Map<String, Object>> getAvailableSlots(Long doctorId, LocalDate date) {
        Doctor doctor = getDoctorById(doctorId);

        // Check if doctor works on this day of week
        String dayOfWeekShort = date.getDayOfWeek().name().substring(0, 3); // MON, TUE, etc.
        if (doctor.getAvailableDays() != null && !doctor.getAvailableDays().contains(dayOfWeekShort)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> slots = new ArrayList<>();
        LocalTime current = doctor.getSlotStartTime() != null ? doctor.getSlotStartTime() : LocalTime.of(9, 0);
        LocalTime end = doctor.getSlotEndTime() != null ? doctor.getSlotEndTime() : LocalTime.of(17, 0);
        int duration = doctor.getSlotDurationMinutes() != null && doctor.getSlotDurationMinutes() > 0 ? doctor.getSlotDurationMinutes() : 30;

        LocalDateTime now = LocalDateTime.now();

        while (current.plusMinutes(duration).isBefore(end) || current.plusMinutes(duration).equals(end)) {
            LocalDateTime slotDateTime = LocalDateTime.of(date, current);
            boolean isBooked = appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatusNotCancelled(doctorId, slotDateTime);
            boolean isPast = slotDateTime.isBefore(now);

            Map<String, Object> slot = new HashMap<>();
            slot.put("time", current.toString());
            slot.put("dateTime", slotDateTime.toString());
            slot.put("available", !isBooked && !isPast);
            slots.add(slot);

            current = current.plusMinutes(duration);
        }

        return slots;
    }

    private void initSampleDoctors() {
        List<Doctor> samples = Arrays.asList(
                new Doctor("Dr. Sarah Jenkins, MD", "Cardiology", "MD (Cardiology), FACC", "HeartCare Specialty Clinic", "Suite 401, Metro Health Tower, New York, NY", "+1 (555) 234-5678", BigDecimal.valueOf(120.00), "MON,TUE,WED,THU,FRI", LocalTime.of(9, 0), LocalTime.of(16, 0), 30),
                new Doctor("Dr. Rajan Patel, MBBS, MD", "General Medicine", "MBBS, MD (Internal Medicine)", "City Wellness Center", "124 Park Avenue, Suite 12, Chicago, IL", "+1 (555) 345-6789", BigDecimal.valueOf(75.00), "MON,TUE,WED,THU,FRI,SAT", LocalTime.of(8, 30), LocalTime.of(17, 0), 30),
                new Doctor("Dr. Emily Chen, MD", "Endocrinology & Diabetology", "MD, FACE - Harvard Med", "Chen Metabolic Health Institute", "500 Commonwealth Ave, Boston, MA", "+1 (555) 456-7890", BigDecimal.valueOf(140.00), "MON,WED,THU", LocalTime.of(10, 0), LocalTime.of(16, 0), 30),
                new Doctor("Dr. Marcus Brody, DO", "Neurology", "DO, Board Certified Neurologist", "NeuroCare Specialists", "88 Medical Plaza, San Francisco, CA", "+1 (555) 567-8901", BigDecimal.valueOf(150.00), "TUE,THU,FRI", LocalTime.of(9, 0), LocalTime.of(15, 0), 45),
                new Doctor("Dr. Priya Sharma, MD", "Pediatrics & Family Care", "MD, FAAP", "Sunshine Family Health", "34 Elm Street, Austin, TX", "+1 (555) 678-9012", BigDecimal.valueOf(85.00), "MON,TUE,WED,THU,FRI", LocalTime.of(9, 0), LocalTime.of(17, 30), 30)
        );
        doctorRepository.saveAll(samples);
    }
}
