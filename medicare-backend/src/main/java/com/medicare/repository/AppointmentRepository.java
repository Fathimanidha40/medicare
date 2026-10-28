package com.medicare.repository;

import com.medicare.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByUserIdOrderByAppointmentDateTimeDesc(Long userId);

    List<Appointment> findByUserIdAndAppointmentDateTimeAfterOrderByAppointmentDateTimeAsc(Long userId, LocalDateTime after);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDateTime = :dateTime AND a.status <> 'CANCELLED'")
    boolean existsByDoctorIdAndAppointmentDateTimeAndStatusNotCancelled(@Param("doctorId") Long doctorId, @Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDateTime = :dateTime AND a.status <> 'CANCELLED' AND a.id <> :appointmentId")
    boolean existsByDoctorIdAndAppointmentDateTimeAndStatusNotCancelledExcludingId(@Param("doctorId") Long doctorId, @Param("dateTime") LocalDateTime dateTime, @Param("appointmentId") Long appointmentId);
}
