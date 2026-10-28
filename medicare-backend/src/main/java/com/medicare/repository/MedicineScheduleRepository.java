package com.medicare.repository;

import com.medicare.entity.MedicineSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicineScheduleRepository extends JpaRepository<MedicineSchedule, Long> {
    List<MedicineSchedule> findByMedicineId(Long medicineId);

    @Query("SELECT s FROM MedicineSchedule s WHERE s.medicine.user.id = :userId AND s.medicine.active = true")
    List<MedicineSchedule> findByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM MedicineSchedule s WHERE s.reminderActive = true AND s.medicine.active = true")
    List<MedicineSchedule> findAllActiveSchedules();
}
