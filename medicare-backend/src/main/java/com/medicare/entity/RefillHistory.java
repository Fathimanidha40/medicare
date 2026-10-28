package com.medicare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refill_history")
public class RefillHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private Integer quantityAdded;

    @Column(nullable = false)
    private Integer previousStock;

    @Column(nullable = false)
    private Integer newStock;

    @Column(nullable = false, updatable = false)
    private LocalDateTime refillDate = LocalDateTime.now();

    @Column(length = 255)
    private String notes;

    public RefillHistory() {
    }

    public RefillHistory(Medicine medicine, Integer quantityAdded, Integer previousStock, Integer newStock, String notes) {
        this.medicine = medicine;
        this.quantityAdded = quantityAdded;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.notes = notes;
        this.refillDate = LocalDateTime.now();
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

    public Integer getQuantityAdded() {
        return quantityAdded;
    }

    public void setQuantityAdded(Integer quantityAdded) {
        this.quantityAdded = quantityAdded;
    }

    public Integer getPreviousStock() {
        return previousStock;
    }

    public void setPreviousStock(Integer previousStock) {
        this.previousStock = previousStock;
    }

    public Integer getNewStock() {
        return newStock;
    }

    public void setNewStock(Integer newStock) {
        this.newStock = newStock;
    }

    public LocalDateTime getRefillDate() {
        return refillDate;
    }

    public void setRefillDate(LocalDateTime refillDate) {
        this.refillDate = refillDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
