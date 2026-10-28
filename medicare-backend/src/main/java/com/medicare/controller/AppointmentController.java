package com.medicare.controller;

import com.medicare.dto.AppointmentRequest;
import com.medicare.entity.Appointment;
import com.medicare.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<Appointment>> getUserAppointments(Authentication auth) {
        List<Appointment> list = appointmentService.getUserAppointments(auth.getName());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<Appointment>> getUpcomingAppointments(Authentication auth) {
        List<Appointment> list = appointmentService.getUpcomingAppointments(auth.getName());
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<Appointment> bookAppointment(Authentication auth, @Valid @RequestBody AppointmentRequest req) {
        Appointment booked = appointmentService.bookAppointment(auth.getName(), req);
        return new ResponseEntity<>(booked, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<Appointment> rescheduleAppointment(
            @PathVariable Long id,
            Authentication auth,
            @RequestParam("dateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDateTime) {
        Appointment rescheduled = appointmentService.rescheduleAppointment(id, auth.getName(), newDateTime);
        return ResponseEntity.ok(rescheduled);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelAppointment(@PathVariable Long id, Authentication auth) {
        appointmentService.cancelAppointment(id, auth.getName());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Appointment cancelled successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Appointment> updateAppointment(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody AppointmentRequest req) {
        Appointment updated = appointmentService.updateAppointment(id, auth.getName(), req);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteAppointment(
            @PathVariable Long id,
            Authentication auth) {
        appointmentService.deleteAppointment(id, auth.getName());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Appointment deleted successfully");
        return ResponseEntity.ok(response);
    }
}
