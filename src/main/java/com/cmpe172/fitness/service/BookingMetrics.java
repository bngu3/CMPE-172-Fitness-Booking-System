package com.cmpe172.fitness.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Metrics recorded for successfully committed booking operations. */
@Component
public class BookingMetrics {

    private final Counter bookingsCreated;

    public BookingMetrics(MeterRegistry meterRegistry) {
        this.bookingsCreated = Counter.builder("fitness.bookings.created")
                .description("Total number of successfully committed bookings")
                .register(meterRegistry);
    }

    public void recordBookingCreated() {
        bookingsCreated.increment();
    }
}
