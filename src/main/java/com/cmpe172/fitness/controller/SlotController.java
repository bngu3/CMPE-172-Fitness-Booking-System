package com.cmpe172.fitness.controller;

import com.cmpe172.fitness.dto.SlotDTO;
import com.cmpe172.fitness.service.SlotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller: entry point for HTTP requests. All requests are routed
 * here through Spring's DispatcherServlet (the Front Controller).
 */
@RestController
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    /**
     * GET /slots
     * Returns all currently available (not booked) slots as JSON DTOs.
     */
    @GetMapping("/slots")
    public List<SlotDTO> getAvailableSlots() {
        return slotService.getAvailableSlots();
    }
}
