package com.cmpe172.fitness.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AppointmentReminderScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentReminderScheduler.class);
    private final AppointmentReminderService reminderService;

    public AppointmentReminderScheduler(AppointmentReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "${fitness.reminders.cron:0 0 9 * * *}")
    public void sendDailyReminders() {
        try {
            var result = reminderService.run(1);
            logger.info("Appointment reminder run finished: scanned={}, sent={}, failed={}",
                    result.scanned(), result.sent(), result.failed());
        } catch (RuntimeException exception) {
            logger.error("Appointment reminder run failed.", exception);
        }
    }
}
