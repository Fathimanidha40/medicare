package com.medicare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "caregiver_relationships", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"patient_id", "caregiver_id"})
})
public class CaregiverRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "caregiver_id", nullable = false)
    private User caregiver;

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, ACCEPTED, REVOKED

    @Column(length = 100)
    private String permissions = "VIEW_DOSES,MISSED_DOSE_ALERTS";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public CaregiverRelationship() {}

    public CaregiverRelationship(User patient, User caregiver, String permissions) {
        this.patient = patient;
        this.caregiver = caregiver;
        this.permissions = permissions;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getPatient() {
        return patient;
    }

    public void setPatient(User patient) {
        this.patient = patient;
    }

    public User getCaregiver() {
        return caregiver;
    }

    public void setCaregiver(User caregiver) {
        this.caregiver = caregiver;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
