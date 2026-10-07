package com.cmpe172.fitness.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RoleDashboardController {

    @GetMapping("/customer/dashboard")
    public String customerDashboard(Authentication authentication, Model model) {
        model.addAttribute("roleLabel", "Customer");
        model.addAttribute("email", authentication.getName());
        return "role-dashboard";
    }

    @GetMapping("/provider/dashboard")
    public String providerDashboard(Authentication authentication, Model model) {
        model.addAttribute("roleLabel", "Provider");
        model.addAttribute("email", authentication.getName());
        return "role-dashboard";
    }
}
