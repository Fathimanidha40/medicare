package com.medicare;

import com.medicare.dto.AdherenceReportDto;
import com.medicare.dto.DashboardSummaryDto;
import com.medicare.dto.MedicineRequest;
import com.medicare.dto.RegisterRequest;
import com.medicare.dto.ScheduleRequest;
import com.medicare.entity.Medicine;
import com.medicare.entity.MedicineSchedule;
import com.medicare.entity.ScheduledDose;
import com.medicare.service.AuthService;
import com.medicare.service.DoseService;
import com.medicare.service.MedicineService;
import com.medicare.service.ReportService;
import com.medicare.service.ScheduleService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private DoseService doseService;

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private AuthService authService;

    private final String email = "reports.patient@example.com";

    @BeforeEach
    void setup() {
        RegisterRequest reg = new RegisterRequest();
        reg.setName("Reports Patient");
        reg.setEmail(email);
        reg.setPassword("Password123!");
        authService.register(reg);

        MedicineRequest medReq = new MedicineRequest();
        medReq.setName("Atorvastatin");
        medReq.setDosage("20mg");
        medReq.setStartDate(LocalDate.now());
        medReq.setTotalStock(40);
        medReq.setQuantityPerDose(1);
        medReq.setActive(true);
        Medicine med = medicineService.createMedicine(email, medReq);

        ScheduleRequest sched1 = new ScheduleRequest();
        sched1.setMedicineId(med.getId());
        sched1.setTimeOfDay(LocalTime.of(8, 0));
        sched1.setDaysOfWeek("ALL");
        MedicineSchedule s1 = scheduleService.createSchedule(email, sched1);
        scheduleService.generateDoseForDate(s1, LocalDate.now());

        ScheduleRequest sched2 = new ScheduleRequest();
        sched2.setMedicineId(med.getId());
        sched2.setTimeOfDay(LocalTime.of(20, 0));
        sched2.setDaysOfWeek("ALL");
        MedicineSchedule s2 = scheduleService.createSchedule(email, sched2);
        scheduleService.generateDoseForDate(s2, LocalDate.now());

        List<ScheduledDose> todayDoses = doseService.getTodayDoses(email);
        Assertions.assertEquals(2, todayDoses.size());

        // Dose 1 is taken
        doseService.markAsTaken(todayDoses.get(0).getId(), email, "Taken morning");

        // Dose 2 is skipped
        doseService.markAsSkipped(todayDoses.get(1).getId(), email, "Skipped evening");
    }

    @Test
    void testAdherenceCalculationFormula() {
        LocalDate today = LocalDate.now();
        AdherenceReportDto report = reportService.getAdherenceReport(email, today, today);

        Assertions.assertNotNull(report);
        Assertions.assertEquals(2, report.getTotalScheduledDoses());
        Assertions.assertEquals(1, report.getTakenDoses());
        Assertions.assertEquals(1, report.getSkippedDoses());

        // 1 taken out of 2 decided = 50.0% adherence rate
        Assertions.assertEquals(50.0, report.getAdherencePercentage());
    }

    @Test
    void testDashboardSummaryMetrics() {
        DashboardSummaryDto summary = reportService.getDashboardSummary(email);
        Assertions.assertNotNull(summary);
        Assertions.assertEquals(1, summary.getTakenCount());
        Assertions.assertEquals(1, summary.getSkippedCount());
        Assertions.assertEquals(50.0, summary.getAdherenceRate());
    }

    @Test
    void testCsvExportGeneration() {
        LocalDate today = LocalDate.now();
        String csv = reportService.generateCsvReport(email, today, today);
        Assertions.assertNotNull(csv);
        Assertions.assertTrue(csv.contains("Log ID,Medicine Name,Dosage,Scheduled Time,Response Time,Action,Stock Deducted,Notes"));
        Assertions.assertTrue(csv.contains("Atorvastatin"));
        Assertions.assertTrue(csv.contains("TAKEN"));
        Assertions.assertTrue(csv.contains("SKIPPED"));
    }
}
