package com.medicare.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "medicine_schedules")
public class MedicineSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private LocalTime timeOfDay;

    @Column(length = 50)
    private String daysOfWeek = "ALL"; // e.g. "MON,WED,FRI" or "ALL"

    private Integer intervalDays = 1;

    private Boolean reminderActive = true;

    private LocalDate cycleStartDate;

    private LocalDate cycleEndDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public MedicineSchedule() {
    }

    public MedicineSchedule(Medicine medicine, LocalTime timeOfDay, String daysOfWeek, Integer intervalDays, Boolean reminderActive, LocalDate cycleStartDate, LocalDate cycleEndDate) {
        this.medicine = medicine;
        this.timeOfDay = timeOfDay;
        this.daysOfWeek = (daysOfWeek != null && !daysOfWeek.isBlank()) ? daysOfWeek : "ALL";
        this.intervalDays = (intervalDays != null && intervalDays > 0) ? intervalDays : 1;
        this.reminderActive = (reminderActive != null) ? reminderActive : true;
        this.cycleStartDate = cycleStartDate;
        this.cycleEndDate = cycleEndDate;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Medicine getMedicine() {
        return medicine;
    }

    public void setMedicine(Medicine medicine) {
        this.medicine = medicine;
    }

    public LocalTime getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(LocalTime timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public String getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(String daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public Boolean getReminderActive() {
        return reminderActive;
    }

    public void setReminderActive(Boolean reminderActive) {
        this.reminderActive = reminderActive;
    }

    public LocalDate getCycleStartDate() {
        return cycleStartDate;
    }

    public void setCycleStartDate(LocalDate cycleStartDate) {
        this.cycleStartDate = cycleStartDate;
    }

    public LocalDate getCycleEndDate() {
        return cycleEndDate;
    }

    public void setCycleEndDate(LocalDate cycleEndDate) {
        this.cycleEndDate = cycleEndDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
