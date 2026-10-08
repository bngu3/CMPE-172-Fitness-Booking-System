package com.cmpe172.fitness.service;

import com.cmpe172.fitness.exception.SlotUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Requires the isolated PostgreSQL booking_app_test database configured in src/test/resources. */
@SpringBootTest
class BookingConcurrencyIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void stopExecutor() {
        executor.shutdownNow();
    }

    @Test
    void exactlyOneOfTwoConcurrentAttemptsBooksTheSameSlot() throws Exception {
        int slotId = 2; // Seeded as future-dated and available in the isolated test database.
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Boolean> first = executor.submit(() -> attemptBooking(
                slotId, "alex.chen@example.com", ready, start));
        Future<Boolean> second = executor.submit(() -> attemptBooking(
                slotId, "jamie.rivera@example.com", ready, start));

        assertTrue(ready.await(5, TimeUnit.SECONDS), "Both booking threads should be ready");
        start.countDown();

        int successfulBookings = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
        assertEquals(1, successfulBookings);

        Integer appointmentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointments WHERE slot_id = ? AND status = 'BOOKED'",
                Integer.class, slotId);
        assertEquals(1, appointmentCount);
    }

    private boolean attemptBooking(int slotId, String email,
                                   CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent booking start signal timed out");
        }
        try {
            bookingService.bookSlot(slotId, email);
            return true;
        } catch (SlotUnavailableException exception) {
            return false;
        }
    }
}
