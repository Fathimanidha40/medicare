package com.medicare.repository;

import com.medicare.entity.ScheduledDose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledDoseRepository extends JpaRepository<ScheduledDose, Long> {
    Optional<ScheduledDose> findByIdAndUserId(Long id, Long userId);

    boolean existsByMedicineIdAndScheduledDateTime(Long medicineId, LocalDateTime scheduledDateTime);

    @Query("SELECT d FROM ScheduledDose d WHERE d.user.id = :userId AND d.scheduledDateTime BETWEEN :start AND :end ORDER BY d.scheduledDateTime ASC")
    List<ScheduledDose> findByUserIdAndScheduledDateTimeBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT d FROM ScheduledDose d WHERE d.user.id = :userId AND d.scheduledDateTime >= :now AND d.status = 'PENDING' ORDER BY d.scheduledDateTime ASC")
    List<ScheduledDose> findUpcomingPendingDoses(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(d) FROM ScheduledDose d WHERE d.user.id = :userId AND d.status = :status AND d.scheduledDateTime BETWEEN :start AND :end")
    long countByUserIdAndStatusAndDateRange(@Param("userId") Long userId, @Param("status") String status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
