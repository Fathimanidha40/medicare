package com.medicare.repository;

import com.medicare.entity.RefillHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefillHistoryRepository extends JpaRepository<RefillHistory, Long> {
    List<RefillHistory> findByMedicineIdOrderByRefillDateDesc(Long medicineId);

    void deleteByMedicineId(Long medicineId);
}
