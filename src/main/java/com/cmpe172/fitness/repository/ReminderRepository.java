package com.cmpe172.fitness.repository;

import com.cmpe172.fitness.dto.ReminderCandidate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ReminderRepository {
    private final JdbcTemplate jdbcTemplate;

    public ReminderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ReminderCandidate> findUpcoming(LocalDateTime through) {
        return jdbcTemplate.query("""
                        SELECT a.appointment_id, u.full_name AS customer_name, u.email AS customer_email,
                               sv.name AS service_name, p.full_name AS provider_name,
                               (s.slot_date + s.start_time) AS starts_at
                        FROM appointments a
                        JOIN users u ON u.user_id = a.user_id
                        JOIN availability_slots s ON s.slot_id = a.slot_id
                        JOIN services sv ON sv.service_id = s.service_id
                        JOIN providers p ON p.provider_id = s.provider_id
                        WHERE a.status = 'BOOKED'
                          AND (s.slot_date + s.start_time) > CURRENT_TIMESTAMP
                          AND (s.slot_date + s.start_time) <= ?
                        ORDER BY s.slot_date, s.start_time
                        """,
                (rs, rowNum) -> new ReminderCandidate(
                        rs.getInt("appointment_id"), rs.getString("customer_name"),
                        rs.getString("customer_email"), rs.getString("service_name"),
                        rs.getString("provider_name"), rs.getTimestamp("starts_at").toLocalDateTime()),
                Timestamp.valueOf(through));
    }
}
