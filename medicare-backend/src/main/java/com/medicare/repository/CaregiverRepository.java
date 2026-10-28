package com.medicare.repository;

import com.medicare.entity.CaregiverRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaregiverRepository extends JpaRepository<CaregiverRelationship, Long> {

    List<CaregiverRelationship> findByPatientId(Long patientId);

    List<CaregiverRelationship> findByCaregiverId(Long caregiverId);

    Optional<CaregiverRelationship> findByPatientIdAndCaregiverId(Long patientId, Long caregiverId);

    boolean existsByPatientIdAndCaregiverIdAndStatus(Long patientId, Long caregiverId, String status);
}
