package com.medicare.repository;

import com.medicare.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByActiveTrue();
    List<Doctor> findBySpecialtyIgnoreCaseAndActiveTrue(String specialty);
    Optional<Doctor> findFirstByNameIgnoreCase(String name);
}
