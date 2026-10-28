package com.medicare.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SnoozeRequest {

    @NotNull(message = "Snooze minutes are required")
    @Min(value = 5, message = "Minimum snooze duration is 5 minutes")
    private Integer minutes = 15;

    public SnoozeRequest() {
    }

    public SnoozeRequest(Integer minutes) {
        this.minutes = minutes;
    }

    public Integer getMinutes() {
        return minutes;
    }

    public void setMinutes(Integer minutes) {
        this.minutes = minutes;
    }
}
