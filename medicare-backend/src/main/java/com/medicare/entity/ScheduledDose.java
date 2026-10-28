package com.medicare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_doses")
public class ScheduledDose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedule_id")
    private MedicineSchedule schedule;

    @Column(nullable = false)
    private LocalDateTime scheduledDateTime;

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, TAKEN, SKIPPED, SNOOZED

    private LocalDateTime actionTimestamp;

    private LocalDateTime snoozeUntil;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ScheduledDose() {
    }

    public ScheduledDose(User user, Medicine medicine, MedicineSchedule schedule, LocalDateTime scheduledDateTime) {
        this.user = user;
        this.medicine = medicine;
        this.schedule = schedule;
        this.scheduledDateTime = scheduledDateTime;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Medicine getMedicine() {
        return medicine;
    }

    public void setMedicine(Medicine medicine) {
        this.medicine = medicine;
    }

    public MedicineSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(MedicineSchedule schedule) {
        this.schedule = schedule;
    }

    public LocalDateTime getScheduledDateTime() {
        return scheduledDateTime;
    }

    public void setScheduledDateTime(LocalDateTime scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getActionTimestamp() {
        return actionTimestamp;
    }

    public void setActionTimestamp(LocalDateTime actionTimestamp) {
        this.actionTimestamp = actionTimestamp;
    }

    public LocalDateTime getSnoozeUntil() {
        return snoozeUntil;
    }

    public void setSnoozeUntil(LocalDateTime snoozeUntil) {
        this.snoozeUntil = snoozeUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
