package com.cmpe172.fitness.service;

import com.cmpe172.fitness.dto.NotificationRequest;
import com.cmpe172.fitness.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;

/** Gateway for the coarse-grained notification service HTTP boundary. */
@Component
public class NotificationClient {

    private static final Logger logger = LoggerFactory.getLogger(NotificationClient.class);
    private static final int MAX_ATTEMPTS = 3;

    private final RestClient restClient;

    public NotificationClient(RestClient.Builder builder,
                              @Value("${notification.service.url:http://localhost:8080}") String serviceUrl,
                              @Value("${notification.service.connect-timeout-ms:500}") long connectTimeoutMs,
                              @Value("${notification.service.read-timeout-ms:1000}") long readTimeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = builder
                .baseUrl(serviceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public boolean send(NotificationRequest request) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                NotificationResponse response = restClient.post()
                        .uri("/internal/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(NotificationResponse.class);
                if (response != null && response.accepted()) {
                    return true;
                }
                logger.warn("Notification service returned no acceptance: type={}, key={}",
                        request.type(), request.idempotencyKey());
                return false;
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().is4xxClientError()) {
                    logger.error("Notification rejected with HTTP {}: type={}, key={}",
                            exception.getStatusCode().value(), request.type(), request.idempotencyKey());
                    return false;
                }
                if (attempt == MAX_ATTEMPTS) {
                    logger.warn("Notification unavailable after {} attempts; booking remains committed: type={}, key={}",
                            MAX_ATTEMPTS, request.type(), request.idempotencyKey());
                    return false;
                }
                if (!pauseBeforeRetry(attempt, request)) {
                    return false;
                }
            } catch (ResourceAccessException exception) {
                if (attempt == MAX_ATTEMPTS) {
                    logger.warn("Notification timeout or connection failure after {} attempts; booking remains committed: type={}, key={}",
                            MAX_ATTEMPTS, request.type(), request.idempotencyKey());
                    return false;
                }
                if (!pauseBeforeRetry(attempt, request)) {
                    return false;
                }
            } catch (RestClientException exception) {
                logger.error("Notification request failed; booking remains committed: type={}, key={}",
                        request.type(), request.idempotencyKey(), exception);
                return false;
            }
        }
        return false;
    }

    private boolean pauseBeforeRetry(int attempt, NotificationRequest request) {
        logger.warn("Notification attempt {} failed; retrying with idempotency key={}",
                attempt, request.idempotencyKey());
        try {
            Thread.sleep(150L * attempt);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.warn("Notification retry interrupted; booking remains committed: type={}, key={}",
                    request.type(), request.idempotencyKey());
            return false;
        }
    }
}
