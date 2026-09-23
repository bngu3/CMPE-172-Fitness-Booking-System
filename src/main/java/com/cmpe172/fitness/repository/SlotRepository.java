package com.cmpe172.fitness.repository;

import com.cmpe172.fitness.dto.SlotDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository layer: talks directly to the database via JdbcTemplate
 * (plain JDBC under the hood - no Hibernate/JPA/ORM).
 */
@Repository
public class SlotRepository {

    private final JdbcTemplate jdbcTemplate;

    public SlotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns all slots that are not yet booked, joined with provider
     * and service info, ordered by date/time.
     */
    public List<SlotDTO> findAvailableSlots() {
        String sql = """
                SELECT s.slot_id, p.full_name AS provider_name, sv.name AS service_name,
                       s.slot_date, s.start_time, s.end_time, sv.price
                FROM availability_slots s
                JOIN providers p ON s.provider_id = p.provider_id
                JOIN services sv ON s.service_id = sv.service_id
                WHERE s.is_booked = FALSE
                ORDER BY s.slot_date, s.start_time
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new SlotDTO(
                rs.getInt("slot_id"),
                rs.getString("provider_name"),
                rs.getString("service_name"),
                rs.getDate("slot_date").toLocalDate(),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime(),
                rs.getDouble("price")
        ));
    }
}
