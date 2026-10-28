package com.medicare.service;

import com.medicare.dto.InviteCaregiverRequest;
import com.medicare.entity.CaregiverRelationship;
import com.medicare.entity.ScheduledDose;
import com.medicare.entity.User;
import com.medicare.exception.BadRequestException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.CaregiverRepository;
import com.medicare.repository.ScheduledDoseRepository;
import com.medicare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CaregiverService {

    private final CaregiverRepository caregiverRepository;
    private final UserRepository userRepository;
    private final ScheduledDoseRepository scheduledDoseRepository;

    public CaregiverService(CaregiverRepository caregiverRepository,
                            UserRepository userRepository,
                            ScheduledDoseRepository scheduledDoseRepository) {
        this.caregiverRepository = caregiverRepository;
        this.userRepository = userRepository;
        this.scheduledDoseRepository = scheduledDoseRepository;
    }

    @Transactional
    public CaregiverRelationship inviteCaregiver(String patientEmail, InviteCaregiverRequest request) {
        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient account not found"));

        if (patient.getEmail().equalsIgnoreCase(request.getCaregiverEmail().trim())) {
            throw new BadRequestException("You cannot add yourself as your own caregiver.");
        }

        User caregiver = userRepository.findByEmail(request.getCaregiverEmail().trim())
                .orElseThrow(() -> new BadRequestException("No registered MediCare patient found with email: " + request.getCaregiverEmail() + ". Please ask them to register an account first."));

        Optional<CaregiverRelationship> existing = caregiverRepository.findByPatientIdAndCaregiverId(patient.getId(), caregiver.getId());
        if (existing.isPresent()) {
            CaregiverRelationship rel = existing.get();
            if ("ACCEPTED".equals(rel.getStatus())) {
                throw new BadRequestException("This caregiver already has active access to your account.");
            }
            rel.setStatus("PENDING");
            rel.setPermissions(request.getPermissions());
            rel.setUpdatedAt(LocalDateTime.now());
            return caregiverRepository.save(rel);
        }

        CaregiverRelationship newRel = new CaregiverRelationship(patient, caregiver, request.getPermissions());
        return caregiverRepository.save(newRel);
    }

    public List<Map<String, Object>> getCaregiversForPatient(String patientEmail) {
        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<CaregiverRelationship> list = caregiverRepository.findByPatientId(patient.getId());
        List<Map<String, Object>> response = new ArrayList<>();
        for (CaregiverRelationship rel : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", rel.getId());
            map.put("caregiverId", rel.getCaregiver().getId());
            map.put("caregiverName", rel.getCaregiver().getName());
            map.put("caregiverEmail", rel.getCaregiver().getEmail());
            map.put("status", rel.getStatus());
            map.put("permissions", rel.getPermissions());
            map.put("createdAt", rel.getCreatedAt());
            response.add(map);
        }
        return response;
    }

    public List<Map<String, Object>> getPatientsForCaregiver(String caregiverEmail) {
        User caregiver = userRepository.findByEmail(caregiverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<CaregiverRelationship> list = caregiverRepository.findByCaregiverId(caregiver.getId());
        List<Map<String, Object>> response = new ArrayList<>();
        for (CaregiverRelationship rel : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", rel.getId());
            map.put("patientId", rel.getPatient().getId());
            map.put("patientName", rel.getPatient().getName());
            map.put("patientEmail", rel.getPatient().getEmail());
            map.put("status", rel.getStatus());
            map.put("permissions", rel.getPermissions());
            map.put("createdAt", rel.getCreatedAt());
            response.add(map);
        }
        return response;
    }

    @Transactional
    public CaregiverRelationship acceptInvitation(String caregiverEmail, Long relationshipId) {
        User caregiver = userRepository.findByEmail(caregiverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CaregiverRelationship rel = caregiverRepository.findById(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation record not found"));

        if (!rel.getCaregiver().getId().equals(caregiver.getId())) {
            throw new BadRequestException("You are not authorized to respond to this invitation.");
        }

        rel.setStatus("ACCEPTED");
        return caregiverRepository.save(rel);
    }

    @Transactional
    public void revokeAccess(String userEmail, Long relationshipId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CaregiverRelationship rel = caregiverRepository.findById(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Caregiver relationship not found"));

        if (!rel.getPatient().getId().equals(user.getId()) && !rel.getCaregiver().getId().equals(user.getId())) {
            throw new BadRequestException("You are not authorized to modify this relationship.");
        }

        rel.setStatus("REVOKED");
        caregiverRepository.save(rel);
    }

    public Map<String, Object> getPatientDoseSummary(String caregiverEmail, Long patientId) {
        User caregiver = userRepository.findByEmail(caregiverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean authorized = caregiverRepository.existsByPatientIdAndCaregiverIdAndStatus(patientId, caregiver.getId(), "ACCEPTED");
        if (!authorized) {
            throw new BadRequestException("Access denied: You are not an authorized caregiver for this patient.");
        }

        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<ScheduledDose> todayDoses = scheduledDoseRepository.findByUserIdAndScheduledDateTimeBetween(
                patientId, startOfDay, endOfDay
        );

        LocalDateTime now = LocalDateTime.now();
        int taken = 0;
        int skipped = 0;
        int pending = 0;
        int confirmedMissed = 0; // Past scheduled time by > 60 mins and still pending

        List<Map<String, Object>> doseItems = new ArrayList<>();
        for (ScheduledDose d : todayDoses) {
            boolean isOverdue = d.getStatus().equals("PENDING") && d.getScheduledDateTime().plusHours(1).isBefore(now);
            if (d.getStatus().equals("TAKEN")) taken++;
            else if (d.getStatus().equals("SKIPPED")) skipped++;
            else {
                pending++;
                if (isOverdue) confirmedMissed++;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId());
            item.put("medicineName", d.getMedicine().getName());
            item.put("dosage", d.getMedicine().getDosage());
            item.put("scheduledTime", d.getScheduledDateTime());
            item.put("status", d.getStatus());
            item.put("isOverdue", isOverdue);
            item.put("actionTimestamp", d.getActionTimestamp());
            doseItems.add(item);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("patientId", patient.getId());
        summary.put("patientName", patient.getName());
        summary.put("takenCount", taken);
        summary.put("skippedCount", skipped);
        summary.put("pendingCount", pending);
        summary.put("confirmedMissedCount", confirmedMissed);
        summary.put("doses", doseItems);

        return summary;
    }
}
