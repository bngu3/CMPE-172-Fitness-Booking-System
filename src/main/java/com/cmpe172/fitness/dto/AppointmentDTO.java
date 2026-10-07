package com.cmpe172.fitness.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** A customer's appointment row for the upcoming or history list. */
public record AppointmentDTO(
        int appointmentId,
        String serviceName,
        String providerName,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal price,
        String status) { }
