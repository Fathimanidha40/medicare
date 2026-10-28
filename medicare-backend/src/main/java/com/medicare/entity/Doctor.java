package com.medicare.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String specialty;

    @Column(nullable = false, length = 150)
    private String qualification;

    @Column(nullable = false, length = 150)
    private String clinicName;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 30)
    private String contactNumber;

    @Column(precision = 10, scale = 2)
    private BigDecimal consultationFee = BigDecimal.ZERO;

    private String avatarUrl;

    @Column(length = 100)
    private String availableDays = "MON,TUE,WED,THU,FRI";

    private LocalTime slotStartTime = LocalTime.of(9, 0);

    private LocalTime slotEndTime = LocalTime.of(17, 0);

    private Integer slotDurationMinutes = 30;

    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Doctor() {
    }

    public Doctor(String name, String specialty, String qualification, String clinicName, String address, String contactNumber, BigDecimal consultationFee, String availableDays, LocalTime slotStartTime, LocalTime slotEndTime, Integer slotDurationMinutes) {
        this.name = name;
        this.specialty = specialty;
        this.qualification = qualification;
        this.clinicName = clinicName;
        this.address = address;
        this.contactNumber = contactNumber;
        this.consultationFee = consultationFee;
        this.availableDays = availableDays;
        this.slotStartTime = slotStartTime;
        this.slotEndTime = slotEndTime;
        this.slotDurationMinutes = slotDurationMinutes;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public String getClinicName() {
        return clinicName;
    }

    public void setClinicName(String clinicName) {
        this.clinicName = clinicName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(String availableDays) {
        this.availableDays = availableDays;
    }

    public LocalTime getSlotStartTime() {
        return slotStartTime;
    }

    public void setSlotStartTime(LocalTime slotStartTime) {
        this.slotStartTime = slotStartTime;
    }

    public LocalTime getSlotEndTime() {
        return slotEndTime;
    }

    public void setSlotEndTime(LocalTime slotEndTime) {
        this.slotEndTime = slotEndTime;
    }

    public Integer getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(Integer slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
