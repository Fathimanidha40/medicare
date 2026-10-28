package com.medicare.service;

import com.medicare.dto.SnoozeRequest;
import com.medicare.entity.*;
import com.medicare.exception.BadRequestException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class DoseService {

    private final ScheduledDoseRepository doseRepository;
    private final DoseLogRepository doseLogRepository;
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;

    public DoseService(ScheduledDoseRepository doseRepository,
                       DoseLogRepository doseLogRepository,
                       MedicineRepository medicineRepository,
                       UserRepository userRepository) {
        this.doseRepository = doseRepository;
        this.doseLogRepository = doseLogRepository;
        this.medicineRepository = medicineRepository;
        this.userRepository = userRepository;
    }

    public List<ScheduledDose> getTodayDoses(String userEmail) {
        User user = getUserByEmail(userEmail);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return doseRepository.findByUserIdAndScheduledDateTimeBetween(user.getId(), startOfDay, endOfDay);
    }

    public List<ScheduledDose> getUpcomingDoses(String userEmail) {
        User user = getUserByEmail(userEmail);
        return doseRepository.findUpcomingPendingDoses(user.getId(), LocalDateTime.now());
    }

    @Transactional
    public ScheduledDose markAsTaken(Long doseId, String userEmail, String notes) {
        ScheduledDose dose = getAuthorizedDose(doseId, userEmail);

        if ("TAKEN".equalsIgnoreCase(dose.getStatus())) {
            throw new BadRequestException("This dose has already been marked as Taken.");
        }

        LocalDateTime now = LocalDateTime.now();
        dose.setStatus("TAKEN");
        dose.setActionTimestamp(now);
        dose.setSnoozeUntil(null);

        // Deduct inventory safely
        Medicine medicine = dose.getMedicine();
        int deductQty = (medicine.getQuantityPerDose() != null && medicine.getQuantityPerDose() > 0)
                ? medicine.getQuantityPerDose()
                : 1;

        int currentStock = medicine.getTotalStock() != null ? medicine.getTotalStock() : 0;
        int newStock = Math.max(0, currentStock - deductQty);
        medicine.setTotalStock(newStock);
        medicineRepository.save(medicine);

        // Create DoseLog audit record
        DoseLog log = new DoseLog(
                dose,
                dose.getUser(),
                medicine,
                dose.getScheduledDateTime(),
                now,
                "TAKEN",
                deductQty,
                notes != null ? notes : "Taken as scheduled"
        );
        doseLogRepository.save(log);

        return doseRepository.save(dose);
    }

    @Transactional
    public ScheduledDose markAsSkipped(Long doseId, String userEmail, String reason) {
        ScheduledDose dose = getAuthorizedDose(doseId, userEmail);

        if ("TAKEN".equalsIgnoreCase(dose.getStatus())) {
            throw new BadRequestException("A dose marked as Taken cannot be changed to Skipped.");
        }

        LocalDateTime now = LocalDateTime.now();
        dose.setStatus("SKIPPED");
        dose.setActionTimestamp(now);
        dose.setSnoozeUntil(null);

        // Do not deduct stock for skipped doses
        DoseLog log = new DoseLog(
                dose,
                dose.getUser(),
                dose.getMedicine(),
                dose.getScheduledDateTime(),
                now,
                "SKIPPED",
                0,
                reason != null ? reason : "Skipped by patient"
        );
        doseLogRepository.save(log);

        return doseRepository.save(dose);
    }

    @Transactional
    public ScheduledDose snoozeDose(Long doseId, String userEmail, SnoozeRequest req) {
        ScheduledDose dose = getAuthorizedDose(doseId, userEmail);

        if ("TAKEN".equalsIgnoreCase(dose.getStatus())) {
            throw new BadRequestException("Cannot snooze a dose that has already been taken.");
        }

        int mins = (req != null && req.getMinutes() != null) ? req.getMinutes() : 15;
        LocalDateTime snoozeUntil = LocalDateTime.now().plusMinutes(mins);

        dose.setStatus("SNOOZED");
        dose.setSnoozeUntil(snoozeUntil);

        return doseRepository.save(dose);
    }

    @Transactional
    public ScheduledDose updateDose(Long doseId, String userEmail, LocalDateTime scheduledDateTime, String status, String notes) {
        ScheduledDose dose = getAuthorizedDose(doseId, userEmail);
        if (scheduledDateTime != null) {
            dose.setScheduledDateTime(scheduledDateTime);
        }
        if (status != null && !status.isBlank()) {
            String upper = status.trim().toUpperCase();
            if ("TAKEN".equals(upper)) {
                return markAsTaken(doseId, userEmail, notes);
            } else if ("SKIPPED".equals(upper)) {
                return markAsSkipped(doseId, userEmail, notes);
            } else if ("PENDING".equals(upper)) {
                dose.setStatus("PENDING");
                dose.setActionTimestamp(null);
                dose.setSnoozeUntil(null);
            }
        }
        return doseRepository.save(dose);
    }

    @Transactional
    public void deleteDose(Long doseId, String userEmail) {
        ScheduledDose dose = getAuthorizedDose(doseId, userEmail);
        doseLogRepository.deleteByScheduledDoseId(dose.getId());
        doseRepository.delete(dose);
    }

    public List<DoseLog> getUserDoseLogs(String userEmail, LocalDate startDate, LocalDate endDate) {
        User user = getUserByEmail(userEmail);
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            return doseLogRepository.findByUserIdAndResponseTimeBetween(user.getId(), start, end);
        }
        return doseLogRepository.findByUserIdOrderByResponseTimeDesc(user.getId());
    }

    private ScheduledDose getAuthorizedDose(Long doseId, String userEmail) {
        User user = getUserByEmail(userEmail);
        ScheduledDose dose = doseRepository.findById(doseId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled dose not found with ID: " + doseId));

        if (!dose.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied to requested dose");
        }
        return dose;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
