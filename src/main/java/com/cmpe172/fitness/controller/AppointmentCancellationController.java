package com.cmpe172.fitness.controller;

import com.cmpe172.fitness.dto.NotificationRequest;
import com.cmpe172.fitness.service.BookingService;
import com.cmpe172.fitness.service.NotificationClient;
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
    private final NotificationClient notificationClient;

    public AppointmentCancellationController(BookingService bookingService, NotificationClient notificationClient) {
        this.bookingService = bookingService;
        this.notificationClient = notificationClient;
    }

    @PostMapping("/customer/appointments/{appointmentId}/cancel")
    public String cancelAppointment(@PathVariable @Positive int appointmentId, Authentication authentication) {
        bookingService.cancelAppointment(appointmentId, authentication.getName());
        notificationClient.send(new NotificationRequest(
                "appointment-" + appointmentId + ":CANCELLED",
                authentication.getName(),
                "BOOKING_CANCELLED",
                "Your booking has been cancelled. Appointment #" + appointmentId + "."));
        return "redirect:/customer/appointments?cancelled";
    }
}
