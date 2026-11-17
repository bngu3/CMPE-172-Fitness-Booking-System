package com.cmpe172.fitness.dto;

import java.time.LocalDateTime;

public record ReminderCandidate(int appointmentId, String customerName, String customerEmail,
                                String serviceName, String providerName, LocalDateTime startsAt) { }
