package com.cmpe172.fitness.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Booking information visible to the provider who owns the slot. */
public record ProviderAppointmentDTO(
        int appointmentId,
        String customerName,
        String serviceName,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        String status) { }
