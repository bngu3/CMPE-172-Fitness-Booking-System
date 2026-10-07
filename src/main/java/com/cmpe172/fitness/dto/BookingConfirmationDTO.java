package com.cmpe172.fitness.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Details shown to the customer after a booking succeeds. */
public record BookingConfirmationDTO(
        int appointmentId,
        String serviceName,
        String providerName,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal price) { }
