package com.cmpe172.fitness.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FitnessAssistantPageController {

    @GetMapping("/customer/assistant")
    public String assistant() {
        return "fitness-assistant";
    }
}
