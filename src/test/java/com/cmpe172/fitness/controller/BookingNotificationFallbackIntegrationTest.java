package com.cmpe172.fitness.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Requires the isolated PostgreSQL booking_app_test database configured in test resources. */
@SpringBootTest(properties = {
        "notification.service.url=http://127.0.0.1:1",
        "notification.service.connect-timeout-ms=100",
        "notification.service.read-timeout-ms=100"
})
class BookingNotificationFallbackIntegrationTest {

    @Autowired
    private BookingController bookingController;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void bookingCommitsWhenNotificationServiceIsUnavailable() {
        int slotId = jdbcTemplate.queryForObject("""
                INSERT INTO availability_slots
                    (provider_id, service_id, slot_date, start_time, end_time)
                VALUES (1, 1, CURRENT_DATE + 30, '09:00', '10:00')
                RETURNING slot_id
                """, Integer.class);
        var authentication = new UsernamePasswordAuthenticationToken(
                "alex.chen@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        String redirect = bookingController.bookSlot(slotId, authentication);

        assertTrue(redirect.startsWith("redirect:/customer/bookings/"));
        Integer bookedAppointments = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointments WHERE slot_id = ? AND status = 'BOOKED'",
                Integer.class, slotId);
        Boolean slotMarkedBooked = jdbcTemplate.queryForObject(
                "SELECT is_booked FROM availability_slots WHERE slot_id = ?",
                Boolean.class, slotId);
        assertEquals(1, bookedAppointments);
        assertEquals(Boolean.TRUE, slotMarkedBooked);
    }
}
