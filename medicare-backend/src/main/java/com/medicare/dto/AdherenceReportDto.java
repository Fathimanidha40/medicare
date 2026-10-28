package com.medicare.dto;

import com.medicare.entity.DoseLog;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AdherenceReportDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private long totalScheduledDoses;
    private long takenDoses;
    private long skippedDoses;
    private long pendingDoses;
    private double adherencePercentage;
    private Map<String, Long> dailyTakenCounts;
    private Map<String, Long> dailySkippedCounts;
    private List<DoseLog> recentLogs;

    public AdherenceReportDto() {
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

    public long getTotalScheduledDoses() {
        return totalScheduledDoses;
    }

    public void setTotalScheduledDoses(long totalScheduledDoses) {
        this.totalScheduledDoses = totalScheduledDoses;
    }

    public long getTakenDoses() {
        return takenDoses;
    }

    public void setTakenDoses(long takenDoses) {
        this.takenDoses = takenDoses;
    }

    public long getSkippedDoses() {
        return skippedDoses;
    }

    public void setSkippedDoses(long skippedDoses) {
        this.skippedDoses = skippedDoses;
    }

    public long getPendingDoses() {
        return pendingDoses;
    }

    public void setPendingDoses(long pendingDoses) {
        this.pendingDoses = pendingDoses;
    }

    public double getAdherencePercentage() {
        return adherencePercentage;
    }

    public void setAdherencePercentage(double adherencePercentage) {
        this.adherencePercentage = adherencePercentage;
    }

    public Map<String, Long> getDailyTakenCounts() {
        return dailyTakenCounts;
    }

    public void setDailyTakenCounts(Map<String, Long> dailyTakenCounts) {
        this.dailyTakenCounts = dailyTakenCounts;
    }

    public Map<String, Long> getDailySkippedCounts() {
        return dailySkippedCounts;
    }

    public void setDailySkippedCounts(Map<String, Long> dailySkippedCounts) {
        this.dailySkippedCounts = dailySkippedCounts;
    }

    public List<DoseLog> getRecentLogs() {
        return recentLogs;
    }

    public void setRecentLogs(List<DoseLog> recentLogs) {
        this.recentLogs = recentLogs;
    }
}
