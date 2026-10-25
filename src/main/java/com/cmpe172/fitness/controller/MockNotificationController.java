package com.cmpe172.fitness.controller;

import com.cmpe172.fitness.dto.NotificationRequest;
import com.cmpe172.fitness.dto.NotificationResponse;
import com.cmpe172.fitness.service.MockNotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoint that represents the external notification system. */
@RestController
@RequestMapping("/internal/notifications")
public class MockNotificationController {

    private final MockNotificationService notificationService;

    public MockNotificationController(MockNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public NotificationResponse receive(@Valid @RequestBody NotificationRequest request) {
        boolean duplicate = notificationService.accept(request);
        return new NotificationResponse(true, duplicate);
    }
}
