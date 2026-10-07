package com.cmpe172.fitness.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

/** Renders the landing page and shows the signed-in user's role-specific link. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        boolean signedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        model.addAttribute("signedIn", signedIn);
        if (signedIn) {
            model.addAttribute("email", authentication.getName());
            model.addAttribute("customer", authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_CUSTOMER")));
            model.addAttribute("provider", authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_PROVIDER")));
        }
        return "home";
    }
}
