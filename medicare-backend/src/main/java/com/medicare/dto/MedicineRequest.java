package com.medicare.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class MedicineRequest {

    @NotBlank(message = "Medicine name is required")
    private String name;

    @NotBlank(message = "Dosage is required (e.g., 500mg, 1 tablet)")
    private String dosage;

    private String unit = "mg";
    private String form = "TABLET"; // TABLET, CAPSULE, LIQUID, INJECTION, INHALER, DROPS, OTHER
    private String instructions;
    private String foodTiming = "AFTER_FOOD"; // BEFORE_FOOD, AFTER_FOOD, WITH_FOOD, ANYTIME

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;
    private String frequency = "DAILY";
    private String prescribingDoctor;

    @Min(value = 0, message = "Total stock cannot be negative")
    private Integer totalStock = 0;

    @Min(value = 1, message = "Quantity per dose must be at least 1")
    private Integer quantityPerDose = 1;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold = 5;

    private String colorTag = "#1977cc";
    private String notes;
    private Boolean active = true;

    public MedicineRequest() {
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
}
