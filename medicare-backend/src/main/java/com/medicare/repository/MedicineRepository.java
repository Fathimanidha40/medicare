package com.medicare.repository;

import com.medicare.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Medicine> findByUserIdAndActiveTrueOrderByNameAsc(Long userId);

    Optional<Medicine> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT m FROM Medicine m WHERE m.user.id = :userId AND m.totalStock <= m.lowStockThreshold AND m.active = true")
    List<Medicine> findLowStockMedicines(@Param("userId") Long userId);
}
