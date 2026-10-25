package com.cmpe172.fitness.controller;

import com.cmpe172.fitness.dto.BookingConfirmationDTO;
import com.cmpe172.fitness.dto.NotificationRequest;
import com.cmpe172.fitness.service.BookingMetrics;
import com.cmpe172.fitness.service.BookingService;
import com.cmpe172.fitness.service.NotificationClient;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Validated
public class BookingController {

    private final BookingService bookingService;
    private final NotificationClient notificationClient;
    private final BookingMetrics bookingMetrics;

    public BookingController(BookingService bookingService, NotificationClient notificationClient,
                             BookingMetrics bookingMetrics) {
        this.bookingService = bookingService;
        this.notificationClient = notificationClient;
        this.bookingMetrics = bookingMetrics;
    }

    @PostMapping("/customer/bookings")
    public String bookSlot(@RequestParam @Positive int slotId, Authentication authentication) {
        int appointmentId = bookingService.bookSlot(slotId, authentication.getName());
        bookingMetrics.recordBookingCreated();
        notificationClient.send(new NotificationRequest(
                "appointment-" + appointmentId + ":BOOKED",
                authentication.getName(),
                "BOOKING_CONFIRMED",
                "Your booking is confirmed. Appointment #" + appointmentId + "."));
        return "redirect:/customer/bookings/" + appointmentId + "/confirmation";
    }

    @GetMapping("/customer/bookings/{appointmentId}/confirmation")
    public String showConfirmation(@PathVariable @Positive int appointmentId,
                                   Authentication authentication,
                                   Model model) {
        BookingConfirmationDTO confirmation = bookingService.getConfirmation(
                appointmentId, authentication.getName());
        model.addAttribute("appointment", confirmation);
        return "booking-confirmation";
    }
}
