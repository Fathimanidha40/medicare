package com.medicare;

import com.medicare.dto.RegisterRequest;
import com.medicare.entity.Prescription;
import com.medicare.exception.BadRequestException;
import com.medicare.service.AuthService;
import com.medicare.service.PrescriptionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class PrescriptionServiceTest {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private AuthService authService;

    private final String user1 = "presc.patient1@example.com";
    private final String user2 = "presc.patient2@example.com";

    @BeforeEach
    void setup() {
        RegisterRequest reg1 = new RegisterRequest();
        reg1.setName("Patient One");
        reg1.setEmail(user1);
        reg1.setPassword("Password123!");
        authService.register(reg1);

        RegisterRequest reg2 = new RegisterRequest();
        reg2.setName("Patient Two");
        reg2.setEmail(user2);
        reg2.setPassword("Password123!");
        authService.register(reg2);
    }

    @Test
    void testUploadAndRetrievePrescriptionSecurely() {
        byte[] pdfBytes = "%PDF-1.4 Mock Prescription Content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dr_jenkins_rx.pdf",
                "application/pdf",
                pdfBytes
        );

        Prescription uploaded = prescriptionService.uploadPrescription(
                user1,
                "Cardiology Clinic Rx",
                "Dr. Sarah Jenkins",
                LocalDate.now(),
                "Follow up in 3 months",
                file
        );

        Assertions.assertNotNull(uploaded.getId());
        Assertions.assertEquals("Cardiology Clinic Rx", uploaded.getTitle());
        Assertions.assertEquals("application/pdf", uploaded.getFileType());

        // Test retrieval by authorized owner
        Resource resource = prescriptionService.loadFileAsResource(uploaded.getId(), user1);
        Assertions.assertTrue(resource.exists());

        // Test that user2 CANNOT access user1's private prescription (HIPAA privacy protection)
        Assertions.assertThrows(AccessDeniedException.class, () ->
                prescriptionService.loadFileAsResource(uploaded.getId(), user2));

        // Test list
        List<Prescription> user1List = prescriptionService.getUserPrescriptions(user1);
        Assertions.assertEquals(1, user1List.size());

        List<Prescription> user2List = prescriptionService.getUserPrescriptions(user2);
        Assertions.assertTrue(user2List.isEmpty());

        // Test delete
        prescriptionService.deletePrescription(uploaded.getId(), user1);
        Assertions.assertTrue(prescriptionService.getUserPrescriptions(user1).isEmpty());
    }

    @Test
    void testRejectInvalidFileType() {
        MockMultipartFile badFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "executable content".getBytes()
        );

        Assertions.assertThrows(BadRequestException.class, () ->
                prescriptionService.uploadPrescription(
                        user1,
                        "Bad File",
                        "Dr. Unknown",
                        LocalDate.now(),
                        "",
                        badFile
                ));
    }
}
