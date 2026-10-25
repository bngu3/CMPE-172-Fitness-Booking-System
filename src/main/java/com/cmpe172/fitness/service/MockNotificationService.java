package com.cmpe172.fitness.service;

import com.cmpe172.fitness.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** In-memory stand-in for an external messaging provider. */
@Service
public class MockNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(MockNotificationService.class);
    private final ConcurrentMap<String, NotificationRequest> accepted = new ConcurrentHashMap<>();

    public boolean accept(NotificationRequest notification) {
        boolean firstDelivery = accepted.putIfAbsent(notification.idempotencyKey(), notification) == null;
        if (firstDelivery) {
            logger.info("Mock notification accepted: type={}, key={}",
                    notification.type(), notification.idempotencyKey());
        } else {
            logger.info("Duplicate mock notification ignored: type={}, key={}",
                    notification.type(), notification.idempotencyKey());
        }
        return !firstDelivery;
    }
}
