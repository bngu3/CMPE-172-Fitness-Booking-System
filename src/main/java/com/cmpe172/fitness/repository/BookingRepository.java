package com.cmpe172.fitness.repository;

import com.cmpe172.fitness.dto.BookingConfirmationDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC operations used by the booking transaction. */
@Repository
public class BookingRepository {

    private final JdbcTemplate jdbcTemplate;

    public BookingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Integer> findCustomerIdByEmail(String email) {
        List<Integer> ids = jdbcTemplate.query(
                "SELECT user_id FROM users WHERE LOWER(email) = LOWER(?)",
                (rs, rowNum) -> rs.getInt("user_id"), email);
        return ids.stream().findFirst();
    }

    /** Must be called inside a transaction; the row lock lasts until commit/rollback. */
    public Optional<LockedSlot> lockSlotForBooking(int slotId) {
        List<LockedSlot> slots = jdbcTemplate.query("""
                        SELECT slot_id, slot_date, is_booked
                        FROM availability_slots
                        WHERE slot_id = ?
                        FOR UPDATE
                        """,
                (rs, rowNum) -> new LockedSlot(
                        rs.getInt("slot_id"),
                        rs.getDate("slot_date").toLocalDate(),
                        rs.getBoolean("is_booked")),
                slotId);
        return slots.stream().findFirst();
    }

    public int markSlotBooked(int slotId) {
        return jdbcTemplate.update(
                "UPDATE availability_slots SET is_booked = TRUE WHERE slot_id = ? AND is_booked = FALSE",
                slotId);
    }

    public int insertAppointment(int slotId, int customerId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO appointments (slot_id, user_id, status)
                VALUES (?, ?, 'BOOKED')
                RETURNING appointment_id
                """, Integer.class, slotId, customerId);
    }

    public Optional<BookingConfirmationDTO> findConfirmation(int appointmentId, String customerEmail) {
        List<BookingConfirmationDTO> results = jdbcTemplate.query("""
                        SELECT a.appointment_id, sv.name AS service_name,
                               p.full_name AS provider_name, s.slot_date,
                               s.start_time, s.end_time, sv.price
                        FROM appointments a
                        JOIN users u ON u.user_id = a.user_id
                        JOIN availability_slots s ON s.slot_id = a.slot_id
                        JOIN providers p ON p.provider_id = s.provider_id
                        JOIN services sv ON sv.service_id = s.service_id
                        WHERE a.appointment_id = ? AND LOWER(u.email) = LOWER(?)
                        """,
                (rs, rowNum) -> new BookingConfirmationDTO(
                        rs.getInt("appointment_id"),
                        rs.getString("service_name"),
                        rs.getString("provider_name"),
                        rs.getDate("slot_date").toLocalDate(),
                        rs.getTime("start_time").toLocalTime(),
                        rs.getTime("end_time").toLocalTime(),
                        rs.getBigDecimal("price")),
                appointmentId, customerEmail);
        return results.stream().findFirst();
    }

    public record LockedSlot(int slotId, LocalDate slotDate, boolean booked) { }
}
