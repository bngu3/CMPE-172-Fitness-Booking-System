package com.cmpe172.fitness.dto;

import jakarta.validation.constraints.NotBlank;

/** Coarse-grained message passed between the scheduler and notification boundary. */
public record NotificationRequest(
        @NotBlank String idempotencyKey,
        @NotBlank String recipient,
        @NotBlank String type,
        @NotBlank String message) { }
