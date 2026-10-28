package com.medicare.service;

import com.medicare.dto.AdherenceReportDto;
import com.medicare.dto.DashboardSummaryDto;
import com.medicare.entity.Appointment;
import com.medicare.entity.DoseLog;
import com.medicare.entity.Medicine;
import com.medicare.entity.ScheduledDose;
import com.medicare.entity.User;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {

    private final ScheduledDoseRepository doseRepository;
    private final DoseLogRepository doseLogRepository;
    private final MedicineRepository medicineRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public ReportService(ScheduledDoseRepository doseRepository,
                         DoseLogRepository doseLogRepository,
                         MedicineRepository medicineRepository,
                         AppointmentRepository appointmentRepository,
                         UserRepository userRepository) {
        this.doseRepository = doseRepository;
        this.doseLogRepository = doseLogRepository;
        this.medicineRepository = medicineRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    public DashboardSummaryDto getDashboardSummary(String userEmail) {
        User user = getUserByEmail(userEmail);
        DashboardSummaryDto summary = new DashboardSummaryDto();
        summary.setUserName(user.getName());

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<ScheduledDose> todayDoses = doseRepository.findByUserIdAndScheduledDateTimeBetween(user.getId(), startOfDay, endOfDay);
        summary.setTodayDoses(todayDoses);

        long taken = todayDoses.stream().filter(d -> "TAKEN".equalsIgnoreCase(d.getStatus())).count();
        long skipped = todayDoses.stream().filter(d -> "SKIPPED".equalsIgnoreCase(d.getStatus())).count();
        long pending = todayDoses.stream().filter(d -> "PENDING".equalsIgnoreCase(d.getStatus()) || "SNOOZED".equalsIgnoreCase(d.getStatus())).count();

        summary.setTakenCount(taken);
        summary.setSkippedCount(skipped);
        summary.setPendingCount(pending);

        // Calculate today's adherence rate
        long totalDecided = taken + skipped;
        double adherence = totalDecided > 0 ? ((double) taken / totalDecided) * 100.0 : 100.0;
        summary.setAdherenceRate(Math.round(adherence * 10.0) / 10.0);

        // Next upcoming dose
        List<ScheduledDose> upcoming = doseRepository.findUpcomingPendingDoses(user.getId(), LocalDateTime.now());
        if (!upcoming.isEmpty()) {
            summary.setNextDose(upcoming.get(0));
        }

        // Upcoming doctor appointments
        List<Appointment> upcomingAppointments = appointmentRepository.findByUserIdAndAppointmentDateTimeAfterOrderByAppointmentDateTimeAsc(
                user.getId(), LocalDateTime.now()
        );
        summary.setUpcomingAppointments(upcomingAppointments);

        // Low stock medicines
        List<Medicine> lowStock = medicineRepository.findLowStockMedicines(user.getId());
        summary.setLowStockMedicines(lowStock);

        return summary;
    }

    public AdherenceReportDto getAdherenceReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        User user = getUserByEmail(userEmail);

        if (startDate == null) {
            startDate = LocalDate.now().minusDays(7);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<ScheduledDose> doses = doseRepository.findByUserIdAndScheduledDateTimeBetween(user.getId(), start, end);
        List<DoseLog> logs = doseLogRepository.findByUserIdAndResponseTimeBetween(user.getId(), start, end);

        long taken = doses.stream().filter(d -> "TAKEN".equalsIgnoreCase(d.getStatus())).count();
        long skipped = doses.stream().filter(d -> "SKIPPED".equalsIgnoreCase(d.getStatus())).count();
        long pending = doses.stream().filter(d -> "PENDING".equalsIgnoreCase(d.getStatus()) || "SNOOZED".equalsIgnoreCase(d.getStatus())).count();
        long total = doses.size();

        // Adherence formula: (taken / (taken + skipped + overdue pending)) * 100
        double adherence = (taken + skipped) > 0 ? ((double) taken / (taken + skipped)) * 100.0 : 100.0;

        Map<String, Long> dailyTaken = new LinkedHashMap<>();
        Map<String, Long> dailySkipped = new LinkedHashMap<>();

        LocalDate curr = startDate;
        while (!curr.isAfter(endDate)) {
            String dayKey = curr.format(DateTimeFormatter.ISO_DATE);
            dailyTaken.put(dayKey, 0L);
            dailySkipped.put(dayKey, 0L);
            curr = curr.plusDays(1);
        }

        for (DoseLog log : logs) {
            String dayKey = log.getResponseTime().toLocalDate().format(DateTimeFormatter.ISO_DATE);
            if ("TAKEN".equalsIgnoreCase(log.getAction()) && dailyTaken.containsKey(dayKey)) {
                dailyTaken.put(dayKey, dailyTaken.get(dayKey) + 1);
            } else if ("SKIPPED".equalsIgnoreCase(log.getAction()) && dailySkipped.containsKey(dayKey)) {
                dailySkipped.put(dayKey, dailySkipped.get(dayKey) + 1);
            }
        }

        AdherenceReportDto dto = new AdherenceReportDto();
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        dto.setTotalScheduledDoses(total);
        dto.setTakenDoses(taken);
        dto.setSkippedDoses(skipped);
        dto.setPendingDoses(pending);
        dto.setAdherencePercentage(Math.round(adherence * 10.0) / 10.0);
        dto.setDailyTakenCounts(dailyTaken);
        dto.setDailySkippedCounts(dailySkipped);
        dto.setRecentLogs(logs);

        return dto;
    }

    public String generateCsvReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        User user = getUserByEmail(userEmail);
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        List<DoseLog> logs = doseLogRepository.findByUserIdAndResponseTimeBetween(
                user.getId(), startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)
        );

        StringBuilder csv = new StringBuilder();
        csv.append("Log ID,Medicine Name,Dosage,Scheduled Time,Response Time,Action,Stock Deducted,Notes\n");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (DoseLog log : logs) {
            csv.append(log.getId()).append(",")
                    .append("\"").append(log.getMedicine() != null ? log.getMedicine().getName().replace("\"", "\"\"") : "").append("\",")
                    .append("\"").append(log.getMedicine() != null ? log.getMedicine().getDosage() : "").append("\",")
                    .append(log.getScheduledTime().format(fmt)).append(",")
                    .append(log.getResponseTime().format(fmt)).append(",")
                    .append(log.getAction()).append(",")
                    .append(log.getStockDeducted()).append(",")
                    .append("\"").append(log.getNotes() != null ? log.getNotes().replace("\"", "\"\"") : "").append("\"\n");
        }

        return csv.toString();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
