package com.medicare.controller;

import com.medicare.dto.ScheduleRequest;
import com.medicare.entity.MedicineSchedule;
import com.medicare.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ResponseEntity<List<MedicineSchedule>> getUserSchedules(Authentication auth) {
        List<MedicineSchedule> list = scheduleService.getUserSchedules(auth.getName());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/medicine/{medicineId}")
    public ResponseEntity<List<MedicineSchedule>> getMedicineSchedules(@PathVariable Long medicineId, Authentication auth) {
        List<MedicineSchedule> list = scheduleService.getMedicineSchedules(medicineId, auth.getName());
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<MedicineSchedule> createSchedule(Authentication auth, @Valid @RequestBody ScheduleRequest req) {
        MedicineSchedule schedule = scheduleService.createSchedule(auth.getName(), req);
        return new ResponseEntity<>(schedule, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<MedicineSchedule> toggleSchedule(@PathVariable Long id, Authentication auth) {
        MedicineSchedule updated = scheduleService.toggleSchedule(id, auth.getName());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSchedule(@PathVariable Long id, Authentication auth) {
        scheduleService.deleteSchedule(id, auth.getName());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Schedule deleted successfully");
        return ResponseEntity.ok(response);
    }
}
