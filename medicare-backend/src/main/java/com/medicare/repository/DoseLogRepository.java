package com.medicare.repository;

import com.medicare.entity.DoseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DoseLogRepository extends JpaRepository<DoseLog, Long> {
    List<DoseLog> findByUserIdOrderByResponseTimeDesc(Long userId);

    @Query("SELECT l FROM DoseLog l WHERE l.user.id = :userId AND l.responseTime BETWEEN :start AND :end ORDER BY l.responseTime DESC")
    List<DoseLog> findByUserIdAndResponseTimeBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(l) FROM DoseLog l WHERE l.user.id = :userId AND l.action = :action AND l.responseTime BETWEEN :start AND :end")
    long countByUserIdAndActionAndResponseTimeBetween(@Param("userId") Long userId, @Param("action") String action, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Modifying
    @Query("DELETE FROM DoseLog l WHERE l.scheduledDose.id = :doseId")
    void deleteByScheduledDoseId(@Param("doseId") Long doseId);
}
