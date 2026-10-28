package com.medicare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dose_logs")
public class DoseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_dose_id")
    private ScheduledDose scheduledDose;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private LocalDateTime scheduledTime;

    @Column(nullable = false)
    private LocalDateTime responseTime;

    @Column(nullable = false, length = 20)
    private String action; // TAKEN, SKIPPED

    private Integer stockDeducted = 0;

    @Column(length = 255)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public DoseLog() {
    }

    public DoseLog(ScheduledDose scheduledDose, User user, Medicine medicine, LocalDateTime scheduledTime, LocalDateTime responseTime, String action, Integer stockDeducted, String notes) {
        this.scheduledDose = scheduledDose;
        this.user = user;
        this.medicine = medicine;
        this.scheduledTime = scheduledTime;
        this.responseTime = responseTime;
        this.action = action;
        this.stockDeducted = (stockDeducted != null) ? stockDeducted : 0;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScheduledDose getScheduledDose() {
        return scheduledDose;
    }

    public void setScheduledDose(ScheduledDose scheduledDose) {
        this.scheduledDose = scheduledDose;
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

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public LocalDateTime getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(LocalDateTime responseTime) {
        this.responseTime = responseTime;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getStockDeducted() {
        return stockDeducted;
    }

    public void setStockDeducted(Integer stockDeducted) {
        this.stockDeducted = stockDeducted;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
