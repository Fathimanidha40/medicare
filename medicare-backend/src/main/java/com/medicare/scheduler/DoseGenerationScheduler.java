package com.medicare.scheduler;

import com.medicare.entity.MedicineSchedule;
import com.medicare.repository.MedicineScheduleRepository;
import com.medicare.service.ScheduleService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DoseGenerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(DoseGenerationScheduler.class);

    private final MedicineScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;

    public DoseGenerationScheduler(MedicineScheduleRepository scheduleRepository, ScheduleService scheduleService) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleService = scheduleService;
    }

    @PostConstruct
    public void initDosesOnStartup() {
        log.info("Running initial dose generation on application startup...");
        generateDailyDoses();
    }

    // Run every day at midnight (and every 4 hours to catch newly updated schedules)
    @Scheduled(cron = "0 0 0/4 * * ?")
    public void generateDailyDoses() {
        log.info("Executing scheduled dose generator for today's reminders...");
        LocalDate today = LocalDate.now();
        List<MedicineSchedule> activeSchedules = scheduleRepository.findAllActiveSchedules();

        int generatedCount = 0;
        for (MedicineSchedule schedule : activeSchedules) {
            try {
                scheduleService.generateDoseForDate(schedule, today);
                generatedCount++;
            } catch (Exception e) {
                log.error("Error generating dose for schedule ID {}: {}", schedule.getId(), e.getMessage());
            }
        }
        log.info("Dose generation completed. Processed {} active schedules.", generatedCount);
    }
}
