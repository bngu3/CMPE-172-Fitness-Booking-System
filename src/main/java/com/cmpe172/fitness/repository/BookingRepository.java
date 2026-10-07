package com.cmpe172.fitness.repository;

import com.cmpe172.fitness.dto.BookingConfirmationDTO;
import com.cmpe172.fitness.dto.AppointmentDTO;
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

    /** Locks only a future, booked appointment belonging to this customer. */
    public Optional<CancellableAppointment> lockAppointmentForCancellation(
            int appointmentId, String customerEmail) {
        List<CancellableAppointment> appointments = jdbcTemplate.query("""
                        SELECT a.appointment_id, a.slot_id
                        FROM appointments a
                        JOIN users u ON u.user_id = a.user_id
                        JOIN availability_slots s ON s.slot_id = a.slot_id
                        WHERE a.appointment_id = ?
                          AND LOWER(u.email) = LOWER(?)
                          AND a.status = 'BOOKED'
                          AND s.slot_date >= CURRENT_DATE
                        FOR UPDATE OF a
                        """,
                (rs, rowNum) -> new CancellableAppointment(
                        rs.getInt("appointment_id"), rs.getInt("slot_id")),
                appointmentId, customerEmail);
        return appointments.stream().findFirst();
    }

    public int markAppointmentCancelled(int appointmentId) {
        return jdbcTemplate.update(
                "UPDATE appointments SET status = 'CANCELLED' WHERE appointment_id = ? AND status = 'BOOKED'",
                appointmentId);
    }

    public int markSlotAvailable(int slotId) {
        return jdbcTemplate.update(
                "UPDATE availability_slots SET is_booked = FALSE WHERE slot_id = ? AND is_booked = TRUE",
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

    public int completePastBookedAppointments(String customerEmail) {
        return jdbcTemplate.update("""
                UPDATE appointments a
                SET status = 'COMPLETED'
                FROM availability_slots s, users u
                WHERE a.slot_id = s.slot_id
                  AND a.user_id = u.user_id
                  AND LOWER(u.email) = LOWER(?)
                  AND a.status = 'BOOKED'
                  AND s.slot_date < CURRENT_DATE
                """, customerEmail);
    }

    public List<AppointmentDTO> findUpcomingAppointments(String customerEmail) {
        return findAppointments("a.status = 'BOOKED' AND s.slot_date >= CURRENT_DATE",
                "s.slot_date, s.start_time", customerEmail);
    }

    public List<AppointmentDTO> findAppointmentHistory(String customerEmail) {
        return findAppointments("(a.status IN ('CANCELLED', 'COMPLETED') OR s.slot_date < CURRENT_DATE)",
                "s.slot_date DESC, s.start_time DESC", customerEmail);
    }

    private List<AppointmentDTO> findAppointments(String categoryCondition, String orderBy,
                                                  String customerEmail) {
        String sql = """
                SELECT a.appointment_id, sv.name AS service_name,
                       p.full_name AS provider_name, s.slot_date,
                       s.start_time, s.end_time, sv.price, a.status
                FROM appointments a
                JOIN users u ON u.user_id = a.user_id
                JOIN availability_slots s ON s.slot_id = a.slot_id
                JOIN providers p ON p.provider_id = s.provider_id
                JOIN services sv ON sv.service_id = s.service_id
                WHERE LOWER(u.email) = LOWER(?) AND %s
                ORDER BY %s
                """.formatted(categoryCondition, orderBy);
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new AppointmentDTO(
                        rs.getInt("appointment_id"),
                        rs.getString("service_name"),
                        rs.getString("provider_name"),
                        rs.getDate("slot_date").toLocalDate(),
                        rs.getTime("start_time").toLocalTime(),
                        rs.getTime("end_time").toLocalTime(),
                        rs.getBigDecimal("price"),
                        rs.getString("status")),
                customerEmail);
    }

    public record LockedSlot(int slotId, LocalDate slotDate, boolean booked) { }
    public record CancellableAppointment(int appointmentId, int slotId) { }
}
