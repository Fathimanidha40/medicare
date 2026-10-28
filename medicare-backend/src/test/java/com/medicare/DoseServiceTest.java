package com.medicare;

import com.medicare.dto.MedicineRequest;
import com.medicare.dto.RegisterRequest;
import com.medicare.dto.ScheduleRequest;
import com.medicare.dto.SnoozeRequest;
import com.medicare.entity.Medicine;
import com.medicare.entity.MedicineSchedule;
import com.medicare.entity.ScheduledDose;
import com.medicare.exception.BadRequestException;
import com.medicare.service.AuthService;
import com.medicare.service.DoseService;
import com.medicare.service.MedicineService;
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
class DoseServiceTest {

    @Autowired
    private DoseService doseService;

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private AuthService authService;

    private final String patientEmail = "dose.patient@example.com";
    private Medicine medicine;
    private ScheduledDose scheduledDose;

    @BeforeEach
    void setup() {
        RegisterRequest reg = new RegisterRequest();
        reg.setName("Dose Patient");
        reg.setEmail(patientEmail);
        reg.setPassword("Password123!");
        authService.register(reg);

        MedicineRequest medReq = new MedicineRequest();
        medReq.setName("Metformin");
        medReq.setDosage("500mg");
        medReq.setUnit("mg");
        medReq.setForm("TABLET");
        medReq.setFoodTiming("WITH_FOOD");
        medReq.setStartDate(LocalDate.now());
        medReq.setTotalStock(30);
        medReq.setQuantityPerDose(2); // 2 tablets per dose
        medReq.setLowStockThreshold(10);
        medReq.setActive(true);

        medicine = medicineService.createMedicine(patientEmail, medReq);

        ScheduleRequest schedReq = new ScheduleRequest();
        schedReq.setMedicineId(medicine.getId());
        schedReq.setTimeOfDay(LocalTime.now().plusHours(1).withMinute(0));
        schedReq.setDaysOfWeek("ALL");
        MedicineSchedule schedule = scheduleService.createSchedule(patientEmail, schedReq);

        scheduleService.generateDoseForDate(schedule, LocalDate.now());
        List<ScheduledDose> todayDoses = doseService.getTodayDoses(patientEmail);
        Assertions.assertFalse(todayDoses.isEmpty());
        scheduledDose = todayDoses.get(0);
    }

    @Test
    void testMarkAsTakenDeductsStockCorrectly() {
        Assertions.assertNotNull(scheduledDose);
        Assertions.assertEquals("PENDING", scheduledDose.getStatus());
        Assertions.assertEquals(30, medicine.getTotalStock());

        // Mark Taken
        ScheduledDose taken = doseService.markAsTaken(scheduledDose.getId(), patientEmail, "Taken with dinner");
        Assertions.assertEquals("TAKEN", taken.getStatus());
        Assertions.assertNotNull(taken.getActionTimestamp());

        // Verify stock decremented by quantityPerDose (30 - 2 = 28)
        Medicine updatedMed = medicineService.getMedicineById(medicine.getId(), patientEmail);
        Assertions.assertEquals(28, updatedMed.getTotalStock());

        // Verify duplicate Taken is rejected
        Assertions.assertThrows(BadRequestException.class, () ->
                doseService.markAsTaken(scheduledDose.getId(), patientEmail, "Duplicate attempt"));
    }

    @Test
    void testMarkAsSkippedDoesNotDeductStock() {
        Assertions.assertEquals(30, medicine.getTotalStock());

        ScheduledDose skipped = doseService.markAsSkipped(scheduledDose.getId(), patientEmail, "Fasting for test");
        Assertions.assertEquals("SKIPPED", skipped.getStatus());

        // Stock should remain unchanged
        Medicine updatedMed = medicineService.getMedicineById(medicine.getId(), patientEmail);
        Assertions.assertEquals(30, updatedMed.getTotalStock());
    }

    @Test
    void testSnoozeDose() {
        SnoozeRequest snoozeReq = new SnoozeRequest(20);
        ScheduledDose snoozed = doseService.snoozeDose(scheduledDose.getId(), patientEmail, snoozeReq);

        Assertions.assertEquals("SNOOZED", snoozed.getStatus());
        Assertions.assertNotNull(snoozed.getSnoozeUntil());
    }
}
