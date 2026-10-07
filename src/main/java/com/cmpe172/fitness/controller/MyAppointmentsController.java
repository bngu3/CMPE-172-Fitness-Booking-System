package com.cmpe172.fitness.controller;

import com.cmpe172.fitness.dto.MyAppointmentsDTO;
import com.cmpe172.fitness.service.BookingService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MyAppointmentsController {

    private final BookingService bookingService;

    public MyAppointmentsController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/customer/appointments")
    public String myAppointments(Authentication authentication, Model model) {
        MyAppointmentsDTO appointments = bookingService.getMyAppointments(authentication.getName());
        model.addAttribute("upcoming", appointments.upcoming());
        model.addAttribute("history", appointments.history());
        return "my-appointments";
    }
}
