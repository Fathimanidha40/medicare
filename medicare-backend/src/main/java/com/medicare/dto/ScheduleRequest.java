package com.medicare.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public class ScheduleRequest {

    @NotNull(message = "Medicine ID is required")
    private Long medicineId;

    @NotNull(message = "Time of day is required")
    private LocalTime timeOfDay;

    private String daysOfWeek = "ALL";
    private Integer intervalDays = 1;
    private Boolean reminderActive = true;
    private LocalDate cycleStartDate;
    private LocalDate cycleEndDate;

    public ScheduleRequest() {
    }

    public Long getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Long medicineId) {
        this.medicineId = medicineId;
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
}
