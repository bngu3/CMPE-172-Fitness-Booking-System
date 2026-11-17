package com.cmpe172.fitness.service;

import com.cmpe172.fitness.dto.NotificationRequest;
import com.cmpe172.fitness.dto.ReminderCandidate;
import com.cmpe172.fitness.dto.ReminderRunResult;
import com.cmpe172.fitness.repository.ReminderRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentReminderService {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentReminderService.class);
    private final ReminderRepository reminderRepository;
    private final ChatClient chatClient;
    private final NotificationClient notificationClient;

    public AppointmentReminderService(ReminderRepository reminderRepository, ChatClient.Builder builder,
                                      NotificationClient notificationClient) {
        this.reminderRepository = reminderRepository;
        this.chatClient = builder.build();
        this.notificationClient = notificationClient;
    }

    public ReminderRunResult run(int lookAheadDays) {
        if (lookAheadDays < 1 || lookAheadDays > 30) {
            throw new IllegalArgumentException("lookAheadDays must be between 1 and 30.");
        }
        List<ReminderCandidate> candidates = reminderRepository.findUpcoming(LocalDateTime.now().plusDays(lookAheadDays));
        int sent = 0;
        for (ReminderCandidate candidate : candidates) {
            try {
                String message = createReminder(candidate);
                boolean accepted = notificationClient.send(new NotificationRequest(
                        "appointment-" + candidate.appointmentId() + ":REMINDER",
                        candidate.customerEmail(), "APPOINTMENT_REMINDER", message));
                if (accepted) sent++;
            } catch (RuntimeException exception) {
                logger.warn("Reminder generation or delivery failed for appointment {}.",
                        candidate.appointmentId(), exception);
            }
        }
        return new ReminderRunResult(candidates.size(), sent, candidates.size() - sent);
    }

    private String createReminder(ReminderCandidate candidate) {
        String result = chatClient.prompt()
                .system("Write a warm, concise personalized fitness appointment reminder in one or two sentences. Include date/time and service. Do not invent policies, equipment, or user details. Address the customer by first name.")
                .user("Customer: " + candidate.customerName() + "\nService: " + candidate.serviceName()
                        + "\nInstructor: " + candidate.providerName() + "\nStarts: " + candidate.startsAt())
                .call().content();
        if (result == null || result.isBlank()) throw new IllegalStateException("Gemini returned an empty reminder.");
        return result.trim();
    }
}
