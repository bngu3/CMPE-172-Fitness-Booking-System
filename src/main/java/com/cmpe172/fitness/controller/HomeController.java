package com.cmpe172.fitness.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple home endpoint - Confirms the app is running and layered correctly.
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "message", "Fitness Studio Booking API is running",
                "docs", "Try GET /slots to see available booking slots"
        );
    }
}
