package com.cmpe172.fitness.repository;

import com.cmpe172.fitness.dto.FilterOption;
import com.cmpe172.fitness.dto.ProviderAppointmentDTO;
import com.cmpe172.fitness.dto.ProviderSlotDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC operations scoped to a provider's own services, slots, and appointments. */
@Repository
public class ProviderRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProviderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Integer> findProviderIdByEmail(String email) {
        List<Integer> ids = jdbcTemplate.query(
                "SELECT provider_id FROM providers WHERE LOWER(email) = LOWER(?)",
                (rs, rowNum) -> rs.getInt("provider_id"), email);
        return ids.stream().findFirst();
    }

    public List<FilterOption> findServices(int providerId) {
        return jdbcTemplate.query(
                "SELECT service_id AS id, name FROM services WHERE provider_id = ? ORDER BY name",
                (rs, rowNum) -> new FilterOption(rs.getInt("id"), rs.getString("name")), providerId);
    }

    public Optional<Integer> insertSlot(int providerId, int serviceId, LocalDate date,
                                        java.time.LocalTime start, java.time.LocalTime end) {
        List<Integer> ids = jdbcTemplate.query("""
                        INSERT INTO availability_slots (provider_id, service_id, slot_date, start_time, end_time)
                        SELECT ?, service_id, ?, ?, ?
                        FROM services
                        WHERE service_id = ? AND provider_id = ?
                        RETURNING slot_id
                        """,
                (rs, rowNum) -> rs.getInt("slot_id"),
                providerId, Date.valueOf(date), Time.valueOf(start), Time.valueOf(end), serviceId, providerId);
        return ids.stream().findFirst();
    }

    public List<ProviderSlotDTO> findSlots(int providerId) {
        return jdbcTemplate.query("""
                        SELECT s.slot_id, sv.name AS service_name, s.slot_date,
                               s.start_time, s.end_time, s.is_booked
                        FROM availability_slots s
                        JOIN services sv ON sv.service_id = s.service_id
                        WHERE s.provider_id = ?
                        ORDER BY s.slot_date, s.start_time
                        """,
                (rs, rowNum) -> {
                    LocalDate slotDate = rs.getDate("slot_date").toLocalDate();
                    boolean booked = rs.getBoolean("is_booked");
                    return new ProviderSlotDTO(
                            rs.getInt("slot_id"),
                            rs.getString("service_name"),
                            slotDate,
                            rs.getTime("start_time").toLocalTime(),
                            rs.getTime("end_time").toLocalTime(),
                            booked,
                            !booked && !slotDate.isBefore(LocalDate.now()));
                },
                providerId);
    }

    public Optional<OwnedSlot> lockOwnedSlot(int providerId, int slotId) {
        List<OwnedSlot> slots = jdbcTemplate.query("""
                        SELECT slot_id, slot_date, is_booked
                        FROM availability_slots
                        WHERE slot_id = ? AND provider_id = ?
                        FOR UPDATE
                        """,
                (rs, rowNum) -> new OwnedSlot(
                        rs.getInt("slot_id"),
                        rs.getDate("slot_date").toLocalDate(),
                        rs.getBoolean("is_booked")),
                slotId, providerId);
        return slots.stream().findFirst();
    }

    public boolean hasAppointments(int slotId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM appointments WHERE slot_id = ?)",
                Boolean.class, slotId);
        return Boolean.TRUE.equals(exists);
    }

    public int deleteUnbookedSlot(int providerId, int slotId) {
        return jdbcTemplate.update("""
                DELETE FROM availability_slots
                WHERE slot_id = ? AND provider_id = ? AND is_booked = FALSE
                """, slotId, providerId);
    }

    public int completePastProviderAppointments(String providerEmail) {
        return jdbcTemplate.update("""
                UPDATE appointments a
                SET status = 'COMPLETED'
                FROM availability_slots s, providers p
                WHERE a.slot_id = s.slot_id
                  AND s.provider_id = p.provider_id
                  AND LOWER(p.email) = LOWER(?)
                  AND a.status = 'BOOKED'
                  AND s.slot_date < CURRENT_DATE
                """, providerEmail);
    }

    public List<ProviderAppointmentDTO> findAppointments(String providerEmail) {
        return jdbcTemplate.query("""
                        SELECT a.appointment_id, u.full_name AS customer_name,
                               sv.name AS service_name, s.slot_date,
                               s.start_time, s.end_time, a.status
                        FROM appointments a
                        JOIN users u ON u.user_id = a.user_id
                        JOIN availability_slots s ON s.slot_id = a.slot_id
                        JOIN providers p ON p.provider_id = s.provider_id
                        JOIN services sv ON sv.service_id = s.service_id
                        WHERE LOWER(p.email) = LOWER(?)
                        ORDER BY s.slot_date DESC, s.start_time DESC
                        """,
                (rs, rowNum) -> new ProviderAppointmentDTO(
                        rs.getInt("appointment_id"),
                        rs.getString("customer_name"),
                        rs.getString("service_name"),
                        rs.getDate("slot_date").toLocalDate(),
                        rs.getTime("start_time").toLocalTime(),
                        rs.getTime("end_time").toLocalTime(),
                        rs.getString("status")),
                providerEmail);
    }

    public record OwnedSlot(int slotId, LocalDate slotDate, boolean booked) { }
}
