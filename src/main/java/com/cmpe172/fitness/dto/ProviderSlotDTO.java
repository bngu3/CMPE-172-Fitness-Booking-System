package com.cmpe172.fitness.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** A provider's own availability entry. */
public record ProviderSlotDTO(
        int slotId,
        String serviceName,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean booked,
        boolean removable) { }
