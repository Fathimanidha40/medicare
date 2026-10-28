package com.medicare.dto;

import com.medicare.entity.Appointment;
import com.medicare.entity.Medicine;
import com.medicare.entity.ScheduledDose;

import java.util.List;

public class DashboardSummaryDto {
    private String userName;
    private long takenCount;
    private long skippedCount;
    private long pendingCount;
    private double adherenceRate;
    private ScheduledDose nextDose;
    private List<ScheduledDose> todayDoses;
    private List<Appointment> upcomingAppointments;
    private List<Medicine> lowStockMedicines;

    public DashboardSummaryDto() {
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getTakenCount() {
        return takenCount;
    }

    public void setTakenCount(long takenCount) {
        this.takenCount = takenCount;
    }

    public long getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(long skippedCount) {
        this.skippedCount = skippedCount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public double getAdherenceRate() {
        return adherenceRate;
    }

    public void setAdherenceRate(double adherenceRate) {
        this.adherenceRate = adherenceRate;
    }

    public ScheduledDose getNextDose() {
        return nextDose;
    }

    public void setNextDose(ScheduledDose nextDose) {
        this.nextDose = nextDose;
    }

    public List<ScheduledDose> getTodayDoses() {
        return todayDoses;
    }

    public void setTodayDoses(List<ScheduledDose> todayDoses) {
        this.todayDoses = todayDoses;
    }

    public List<Appointment> getUpcomingAppointments() {
        return upcomingAppointments;
    }

    public void setUpcomingAppointments(List<Appointment> upcomingAppointments) {
        this.upcomingAppointments = upcomingAppointments;
    }

    public List<Medicine> getLowStockMedicines() {
        return lowStockMedicines;
    }

    public void setLowStockMedicines(List<Medicine> lowStockMedicines) {
        this.lowStockMedicines = lowStockMedicines;
    }
}
