package com.medicare.service;

import com.medicare.dto.ScheduleRequest;
import com.medicare.entity.Medicine;
import com.medicare.entity.MedicineSchedule;
import com.medicare.entity.ScheduledDose;
import com.medicare.entity.User;
import com.medicare.exception.BadRequestException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.MedicineRepository;
import com.medicare.repository.MedicineScheduleRepository;
import com.medicare.repository.ScheduledDoseRepository;
import com.medicare.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduleService {

    private final MedicineScheduleRepository scheduleRepository;
    private final MedicineRepository medicineRepository;
    private final ScheduledDoseRepository doseRepository;
    private final UserRepository userRepository;

    public ScheduleService(MedicineScheduleRepository scheduleRepository,
                           MedicineRepository medicineRepository,
                           ScheduledDoseRepository doseRepository,
                           UserRepository userRepository) {
        this.scheduleRepository = scheduleRepository;
        this.medicineRepository = medicineRepository;
        this.doseRepository = doseRepository;
        this.userRepository = userRepository;
    }

    public List<MedicineSchedule> getUserSchedules(String userEmail) {
        User user = getUserByEmail(userEmail);
        return scheduleRepository.findByUserId(user.getId());
    }

    public List<MedicineSchedule> getMedicineSchedules(Long medicineId, String userEmail) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with ID: " + medicineId));

        User user = getUserByEmail(userEmail);
        if (!medicine.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied to medicine schedules");
        }

        return scheduleRepository.findByMedicineId(medicineId);
    }

    @Transactional
    public MedicineSchedule createSchedule(String userEmail, ScheduleRequest req) {
        User user = getUserByEmail(userEmail);
        Medicine medicine = medicineRepository.findById(req.getMedicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with ID: " + req.getMedicineId()));

        if (!medicine.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied to medicine");
        }

        MedicineSchedule schedule = new MedicineSchedule(
                medicine,
                req.getTimeOfDay(),
                req.getDaysOfWeek(),
                req.getIntervalDays(),
                req.getReminderActive(),
                req.getCycleStartDate(),
                req.getCycleEndDate()
        );

        MedicineSchedule saved = scheduleRepository.save(schedule);

        // Instantly generate today's dose if applicable
        generateDoseForDate(saved, LocalDate.now());

        return saved;
    }

    @Transactional
    public MedicineSchedule toggleSchedule(Long scheduleId, String userEmail) {
        User user = getUserByEmail(userEmail);
        MedicineSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        if (!schedule.getMedicine().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied to schedule");
        }

        schedule.setReminderActive(!Boolean.TRUE.equals(schedule.getReminderActive()));
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId, String userEmail) {
        User user = getUserByEmail(userEmail);
        MedicineSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        if (!schedule.getMedicine().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied to schedule");
        }

        scheduleRepository.delete(schedule);
    }

    public void generateDoseForDate(MedicineSchedule schedule, LocalDate date) {
        Medicine medicine = schedule.getMedicine();
        if (!Boolean.TRUE.equals(medicine.getActive()) || !Boolean.TRUE.equals(schedule.getReminderActive())) {
            return;
        }

        // Check medicine start and end date
        if (date.isBefore(medicine.getStartDate())) {
            return;
        }
        if (medicine.getEndDate() != null && date.isAfter(medicine.getEndDate())) {
            return;
        }

        // Check cycle dates if configured
        if (schedule.getCycleStartDate() != null && date.isBefore(schedule.getCycleStartDate())) {
            return;
        }
        if (schedule.getCycleEndDate() != null && date.isAfter(schedule.getCycleEndDate())) {
            return;
        }

        // Check day of week
        String dayOfWeekShort = date.getDayOfWeek().name().substring(0, 3); // MON, TUE...
        if (!"ALL".equalsIgnoreCase(schedule.getDaysOfWeek()) && !schedule.getDaysOfWeek().contains(dayOfWeekShort)) {
            return;
        }

        LocalDateTime doseDateTime = LocalDateTime.of(date, schedule.getTimeOfDay());

        // Idempotency: prevent duplicates
        if (!doseRepository.existsByMedicineIdAndScheduledDateTime(medicine.getId(), doseDateTime)) {
            ScheduledDose dose = new ScheduledDose(
                    medicine.getUser(),
                    medicine,
                    schedule,
                    doseDateTime
            );
            doseRepository.save(dose);
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
