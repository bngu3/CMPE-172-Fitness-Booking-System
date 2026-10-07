package com.cmpe172.fitness.controller;

import com.cmpe172.fitness.service.ProviderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping("/provider/availability")
    public String availability(Authentication authentication, Model model) {
        String email = authentication.getName();
        model.addAttribute("services", providerService.getServices(email));
        model.addAttribute("slots", providerService.getAvailability(email));
        return "provider-availability";
    }

    @PostMapping("/provider/availability")
    public String createSlot(
            @RequestParam int serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            Authentication authentication) {
        providerService.createSlot(authentication.getName(), serviceId, date, startTime, endTime);
        return "redirect:/provider/availability?created";
    }

    @PostMapping("/provider/availability/{slotId}/delete")
    public String deleteSlot(@PathVariable int slotId, Authentication authentication) {
        providerService.deleteSlot(authentication.getName(), slotId);
        return "redirect:/provider/availability?deleted";
    }

    @GetMapping("/provider/appointments")
    public String appointments(Authentication authentication, Model model) {
        model.addAttribute("appointments", providerService.getAppointments(authentication.getName()));
        return "provider-appointments";
    }
}
