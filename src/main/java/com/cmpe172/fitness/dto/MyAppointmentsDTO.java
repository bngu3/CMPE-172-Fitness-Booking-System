package com.cmpe172.fitness.dto;

import java.util.List;

/** The signed-in customer's current bookings and past/cancelled appointments. */
public record MyAppointmentsDTO(List<AppointmentDTO> upcoming, List<AppointmentDTO> history) { }
