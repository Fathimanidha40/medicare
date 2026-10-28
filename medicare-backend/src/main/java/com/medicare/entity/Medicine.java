package com.medicare.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "medicines")
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 50)
    private String dosage;

    @Column(length = 20)
    private String unit = "mg";

    @Column(nullable = false, length = 30)
    private String form = "TABLET"; // TABLET, CAPSULE, LIQUID, INJECTION, INHALER, DROPS, OTHER

    @Column(length = 255)
    private String instructions;

    @Column(length = 30)
    private String foodTiming = "AFTER_FOOD"; // BEFORE_FOOD, AFTER_FOOD, WITH_FOOD, ANYTIME

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false, length = 50)
    private String frequency = "DAILY";

    @Column(length = 120)
    private String prescribingDoctor;

    private Integer totalStock = 0;

    private Integer quantityPerDose = 1;

    private Integer lowStockThreshold = 5;

    @Column(length = 20)
    private String colorTag = "#1977cc";

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public Medicine() {
    }

    public Medicine(User user, String name, String dosage, String unit, String form, String instructions, String foodTiming, LocalDate startDate, LocalDate endDate, String frequency, String prescribingDoctor, Integer totalStock, Integer quantityPerDose, Integer lowStockThreshold, String colorTag, String notes) {
        this.user = user;
        this.name = name;
        this.dosage = dosage;
        this.unit = (unit != null) ? unit : "mg";
        this.form = (form != null) ? form : "TABLET";
        this.instructions = instructions;
        this.foodTiming = (foodTiming != null) ? foodTiming : "AFTER_FOOD";
        this.startDate = startDate;
        this.endDate = endDate;
        this.frequency = (frequency != null) ? frequency : "DAILY";
        this.prescribingDoctor = prescribingDoctor;
        this.totalStock = (totalStock != null) ? totalStock : 0;
        this.quantityPerDose = (quantityPerDose != null && quantityPerDose > 0) ? quantityPerDose : 1;
        this.lowStockThreshold = (lowStockThreshold != null && lowStockThreshold >= 0) ? lowStockThreshold : 5;
        this.colorTag = (colorTag != null) ? colorTag : "#1977cc";
        this.notes = notes;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getFoodTiming() {
        return foodTiming;
    }

    public void setFoodTiming(String foodTiming) {
        this.foodTiming = foodTiming;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getPrescribingDoctor() {
        return prescribingDoctor;
    }

    public void setPrescribingDoctor(String prescribingDoctor) {
        this.prescribingDoctor = prescribingDoctor;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Integer getQuantityPerDose() {
        return quantityPerDose;
    }

    public void setQuantityPerDose(Integer quantityPerDose) {
        this.quantityPerDose = quantityPerDose;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getColorTag() {
        return colorTag;
    }

    public void setColorTag(String colorTag) {
        this.colorTag = colorTag;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
