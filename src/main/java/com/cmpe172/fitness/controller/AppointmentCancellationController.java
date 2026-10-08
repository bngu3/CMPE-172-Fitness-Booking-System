package com.cmpe172.fitness.controller;

import com.cmpe172.fitness.service.BookingService;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@Validated
public class AppointmentCancellationController {

    private final BookingService bookingService;

    public AppointmentCancellationController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/customer/appointments/{appointmentId}/cancel")
    public String cancelAppointment(@PathVariable @Positive int appointmentId, Authentication authentication) {
        bookingService.cancelAppointment(appointmentId, authentication.getName());
        return "redirect:/customer/appointments?cancelled";
    }
}
