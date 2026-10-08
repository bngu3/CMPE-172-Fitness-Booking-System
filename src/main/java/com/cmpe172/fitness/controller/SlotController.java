package com.cmpe172.fitness.controller;

import com.cmpe172.fitness.dto.SlotDTO;
import com.cmpe172.fitness.service.SlotService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;

/** Handles page and JSON requests for currently available appointment slots. */
@Controller
@Validated
public class SlotController {

    private static final int PAGE_SIZE = 5;

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    /** Renders open slots with optional provider, service, and date filters. */
    @GetMapping("/slots")
    public String getAvailableSlots(
            @RequestParam(required = false) @Positive Integer providerId,
            @RequestParam(required = false) @Positive Integer serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            Authentication authentication,
            Model model) {
        int totalSlots = slotService.countAvailableSlots(providerId, serviceId, date);
        int totalPages = (totalSlots + PAGE_SIZE - 1) / PAGE_SIZE;
        int currentPage = Math.max(0, Math.min(page, Math.max(0, totalPages - 1)));
        int offset = currentPage * PAGE_SIZE;

        model.addAttribute("slots", slotService.getAvailableSlots(
                providerId, serviceId, date, PAGE_SIZE, offset));
        model.addAttribute("providers", slotService.getProviderOptions());
        model.addAttribute("services", slotService.getServiceOptions());
        model.addAttribute("selectedProviderId", providerId);
        model.addAttribute("selectedServiceId", serviceId);
        model.addAttribute("selectedDate", date);
        model.addAttribute("page", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalSlots", totalSlots);
        boolean signedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        model.addAttribute("signedIn", signedIn);
        model.addAttribute("customer", signedIn && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CUSTOMER")));
        return "slots";
    }

    /** JSON endpoint retained for API clients; uses the same SQL filters and paging. */
    @ResponseBody
    @GetMapping("/api/slots")
    public List<SlotDTO> getAvailableSlotsJson(
            @RequestParam(required = false) @Positive Integer providerId,
            @RequestParam(required = false) @Positive Integer serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        int totalSlots = slotService.countAvailableSlots(providerId, serviceId, date);
        int totalPages = (totalSlots + PAGE_SIZE - 1) / PAGE_SIZE;
        int safePage = Math.max(0, Math.min(page, Math.max(0, totalPages - 1)));
        return slotService.getAvailableSlots(providerId, serviceId, date, PAGE_SIZE,
                safePage * PAGE_SIZE);
    }
}
