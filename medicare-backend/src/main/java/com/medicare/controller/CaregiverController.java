package com.medicare.controller;

import com.medicare.dto.InviteCaregiverRequest;
import com.medicare.entity.CaregiverRelationship;
import com.medicare.service.CaregiverService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/caregivers")
public class CaregiverController {

    private final CaregiverService caregiverService;

    public CaregiverController(CaregiverService caregiverService) {
        this.caregiverService = caregiverService;
    }

    @PostMapping("/invite")
    public ResponseEntity<CaregiverRelationship> inviteCaregiver(
            Authentication authentication,
            @Valid @RequestBody InviteCaregiverRequest request) {
        CaregiverRelationship rel = caregiverService.inviteCaregiver(authentication.getName(), request);
        return ResponseEntity.ok(rel);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCaregiverNetwork(Authentication authentication) {
        String email = authentication.getName();
        Map<String, Object> result = new HashMap<>();
        result.put("myCaregivers", caregiverService.getCaregiversForPatient(email));
        result.put("caringForPatients", caregiverService.getPatientsForCaregiver(email));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<CaregiverRelationship> acceptInvitation(
            Authentication authentication,
            @PathVariable Long id) {
        CaregiverRelationship rel = caregiverService.acceptInvitation(authentication.getName(), id);
        return ResponseEntity.ok(rel);
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<Map<String, String>> revokeAccess(
            Authentication authentication,
            @PathVariable Long id) {
        caregiverService.revokeAccess(authentication.getName(), id);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Caregiver access updated.");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/patient/{patientId}/summary")
    public ResponseEntity<Map<String, Object>> getPatientDoseSummary(
            Authentication authentication,
            @PathVariable Long patientId) {
        Map<String, Object> summary = caregiverService.getPatientDoseSummary(authentication.getName(), patientId);
        return ResponseEntity.ok(summary);
    }
}
