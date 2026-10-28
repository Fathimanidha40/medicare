package com.medicare;

import com.medicare.dto.AppointmentRequest;
import com.medicare.dto.RegisterRequest;
import com.medicare.entity.Appointment;
import com.medicare.entity.Doctor;
import com.medicare.exception.BadRequestException;
import com.medicare.service.AppointmentService;
import com.medicare.service.AuthService;
import com.medicare.service.DoctorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AppointmentServiceTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AuthService authService;

    @Test
    void testBookAppointmentAndPreventDoubleBooking() {
        String email = "patient.test@example.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setName("Patient Jane");
        reg.setEmail(email);
        reg.setPassword("SecretPass123");
        authService.register(reg);

        List<Doctor> doctors = doctorService.getAllActiveDoctors();
        Assertions.assertFalse(doctors.isEmpty());
        Doctor doc = doctors.get(0);

        LocalDateTime slotTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);

        AppointmentRequest appReq = new AppointmentRequest();
        appReq.setDoctorId(doc.getId());
        appReq.setAppointmentDateTime(slotTime);
        appReq.setAppointmentType("IN_PERSON");
        appReq.setReason("Routine annual wellness consultation");

        Appointment booked = appointmentService.bookAppointment(email, appReq);
        Assertions.assertNotNull(booked);
        Assertions.assertEquals("BOOKED", booked.getStatus());

        // Test double-booking prevention for same doctor & slot
        Assertions.assertThrows(BadRequestException.class, () -> appointmentService.bookAppointment(email, appReq));
    }
}
