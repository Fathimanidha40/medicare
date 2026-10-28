package com.medicare.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RefillRequest {

    @NotNull(message = "Refill quantity is required")
    @Min(value = 1, message = "Refill quantity must be at least 1")
    private Integer quantity;

    private String notes;

    public RefillRequest() {
    }

    public RefillRequest(Integer quantity, String notes) {
        this.quantity = quantity;
        this.notes = notes;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
