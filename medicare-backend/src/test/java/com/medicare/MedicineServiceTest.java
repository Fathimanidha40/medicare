package com.medicare;

import com.medicare.dto.MedicineRequest;
import com.medicare.dto.RefillRequest;
import com.medicare.dto.RegisterRequest;
import com.medicare.entity.Medicine;
import com.medicare.service.AuthService;
import com.medicare.service.MedicineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class MedicineServiceTest {

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private AuthService authService;

    private final String patientEmail = "med.patient@example.com";

    @BeforeEach
    void setupUser() {
        RegisterRequest reg = new RegisterRequest();
        reg.setName("Med Patient");
        reg.setEmail(patientEmail);
        reg.setPassword("Password123!");
        authService.register(reg);
    }

    @Test
    void testMedicineCrudAndRefillStock() {
        MedicineRequest req = new MedicineRequest();
        req.setName("Amoxicillin");
        req.setDosage("500mg");
        req.setUnit("mg");
        req.setForm("CAPSULE");
        req.setInstructions("Take 1 capsule after breakfast");
        req.setFoodTiming("AFTER_FOOD");
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(10));
        req.setFrequency("DAILY");
        req.setPrescribingDoctor("Dr. Sarah Jenkins");
        req.setTotalStock(20);
        req.setQuantityPerDose(1);
        req.setLowStockThreshold(5);
        req.setColorTag("#1977cc");
        req.setActive(true);

        // 1. Create Medicine
        Medicine created = medicineService.createMedicine(patientEmail, req);
        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals("Amoxicillin", created.getName());
        Assertions.assertEquals(20, created.getTotalStock());

        // 2. Fetch list
        List<Medicine> list = medicineService.getUserMedicines(patientEmail, false, null, null);
        Assertions.assertEquals(1, list.size());

        // 3. Update stock via Refill (id, userEmail, refillReq)
        RefillRequest refill = new RefillRequest(15, "Pharmacy Refill Order #1024");
        Medicine refilled = medicineService.refillStock(created.getId(), patientEmail, refill);
        Assertions.assertEquals(35, refilled.getTotalStock());

        // 4. Update Medicine details (id, userEmail, req)
        req.setName("Amoxicillin Trihydrate");
        Medicine updated = medicineService.updateMedicine(created.getId(), patientEmail, req);
        Assertions.assertEquals("Amoxicillin Trihydrate", updated.getName());

        // 5. Delete Medicine (id, userEmail)
        medicineService.deleteMedicine(created.getId(), patientEmail);
        List<Medicine> afterDelete = medicineService.getUserMedicines(patientEmail, false, null, null);
        Assertions.assertTrue(afterDelete.isEmpty());
    }
}
