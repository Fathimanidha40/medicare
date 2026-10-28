package com.medicare;

import com.medicare.dto.InviteCaregiverRequest;
import com.medicare.dto.RegisterRequest;
import com.medicare.entity.CaregiverRelationship;
import com.medicare.exception.BadRequestException;
import com.medicare.service.AuthService;
import com.medicare.service.CaregiverService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class CaregiverServiceTest {

    @Autowired
    private CaregiverService caregiverService;

    @Autowired
    private AuthService authService;

    private final String patientEmail = "grandpa.patient@example.com";
    private final String caregiverEmail = "sarah.caregiver@example.com";
    private final String strangerEmail = "stranger@example.com";

    private Long patientId;

    @BeforeEach
    void setup() {
        RegisterRequest pReg = new RegisterRequest();
        pReg.setName("Grandpa Patient");
        pReg.setEmail(patientEmail);
        pReg.setPassword("Secret123!");
        patientId = authService.register(pReg).getId();

        RegisterRequest cReg = new RegisterRequest();
        cReg.setName("Sarah Caregiver");
        cReg.setEmail(caregiverEmail);
        cReg.setPassword("Secret123!");
        authService.register(cReg);

        RegisterRequest sReg = new RegisterRequest();
        sReg.setName("Stranger User");
        sReg.setEmail(strangerEmail);
        sReg.setPassword("Secret123!");
        authService.register(sReg);
    }

    @Test
    void testCaregiverInviteAcceptAndDoseSummaryAccess() {
        // 1. Patient invites caregiver
        InviteCaregiverRequest inv = new InviteCaregiverRequest(caregiverEmail, "VIEW_DOSES,MISSED_DOSE_ALERTS");
        CaregiverRelationship rel = caregiverService.inviteCaregiver(patientEmail, inv);
        Assertions.assertNotNull(rel.getId());
        Assertions.assertEquals("PENDING", rel.getStatus());

        // 2. Stranger cannot access patient dose summary
        Assertions.assertThrows(BadRequestException.class, () ->
                caregiverService.getPatientDoseSummary(strangerEmail, patientId));

        // 3. Caregiver accepts invitation
        CaregiverRelationship accepted = caregiverService.acceptInvitation(caregiverEmail, rel.getId());
        Assertions.assertEquals("ACCEPTED", accepted.getStatus());

        // 4. Authorized caregiver can now view patient dose summary
        Map<String, Object> summary = caregiverService.getPatientDoseSummary(caregiverEmail, patientId);
        Assertions.assertNotNull(summary);
        Assertions.assertEquals("Grandpa Patient", summary.get("patientName"));

        // 5. Patient revokes access
        caregiverService.revokeAccess(patientEmail, rel.getId());

        // 6. Access denied after revocation
        Assertions.assertThrows(BadRequestException.class, () ->
                caregiverService.getPatientDoseSummary(caregiverEmail, patientId));
    }
}
