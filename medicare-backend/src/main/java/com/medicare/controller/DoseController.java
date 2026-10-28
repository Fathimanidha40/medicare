package com.medicare.controller;

import com.medicare.dto.SnoozeRequest;
import com.medicare.entity.DoseLog;
import com.medicare.entity.ScheduledDose;
import com.medicare.service.DoseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doses")
public class DoseController {

    private final DoseService doseService;

    public DoseController(DoseService doseService) {
        this.doseService = doseService;
    }

    @GetMapping("/today")
    public ResponseEntity<List<ScheduledDose>> getTodayDoses(Authentication auth) {
        List<ScheduledDose> doses = doseService.getTodayDoses(auth.getName());
        return ResponseEntity.ok(doses);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<ScheduledDose>> getUpcomingDoses(Authentication auth) {
        List<ScheduledDose> doses = doseService.getUpcomingDoses(auth.getName());
        return ResponseEntity.ok(doses);
    }

    @PostMapping("/{id}/taken")
    public ResponseEntity<ScheduledDose> markTaken(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        ScheduledDose dose = doseService.markAsTaken(id, auth.getName(), notes);
        return ResponseEntity.ok(dose);
    }

    @PostMapping("/{id}/skipped")
    public ResponseEntity<ScheduledDose> markSkipped(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        ScheduledDose dose = doseService.markAsSkipped(id, auth.getName(), reason);
        return ResponseEntity.ok(dose);
    }

    @PostMapping("/{id}/snooze")
    public ResponseEntity<ScheduledDose> snoozeDose(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody(required = false) SnoozeRequest req) {
        ScheduledDose dose = doseService.snoozeDose(id, auth.getName(), req != null ? req : new SnoozeRequest(15));
        return ResponseEntity.ok(dose);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<DoseLog>> getDoseLogs(
            Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DoseLog> logs = doseService.getUserDoseLogs(auth.getName(), startDate, endDate);
        return ResponseEntity.ok(logs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduledDose> updateDose(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody Map<String, String> body) {
        String timeStr = body != null ? body.get("scheduledDateTime") : null;
        String status = body != null ? body.get("status") : null;
        String notes = body != null ? body.get("notes") : null;

        LocalDateTime scheduledDateTime = null;
        if (timeStr != null && !timeStr.isBlank()) {
            scheduledDateTime = LocalDateTime.parse(timeStr.replace("Z", ""));
        }

        ScheduledDose updated = doseService.updateDose(id, auth.getName(), scheduledDateTime, status, notes);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDose(
            @PathVariable Long id,
            Authentication auth) {
        doseService.deleteDose(id, auth.getName());
        Map<String, String> response = Map.of("message", "Scheduled dose deleted successfully");
        return ResponseEntity.ok(response);
    }
}
