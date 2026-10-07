package com.cmpe172.fitness.repository;

import com.cmpe172.fitness.dto.FilterOption;
import com.cmpe172.fitness.dto.SlotDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
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

    public List<SlotDTO> findAvailableSlots(Integer providerId, Integer serviceId,
                                            LocalDate date, int limit, int offset) {
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT s.slot_id, p.full_name AS provider_name, sv.name AS service_name,
                       s.slot_date, s.start_time, s.end_time, sv.price
                FROM availability_slots s
                JOIN providers p ON s.provider_id = p.provider_id
                JOIN services sv ON s.service_id = sv.service_id
                WHERE s.is_booked = FALSE AND s.slot_date >= CURRENT_DATE
                """
        );
        appendFilters(sql, parameters, providerId, serviceId, date);
        sql.append(" ORDER BY s.slot_date, s.start_time LIMIT ? OFFSET ?");
        parameters.add(limit);
        parameters.add(offset);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new SlotDTO(
                rs.getInt("slot_id"),
                rs.getString("provider_name"),
                rs.getString("service_name"),
                rs.getDate("slot_date").toLocalDate(),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime(),
                rs.getDouble("price")
        ), parameters.toArray());
    }

    public int countAvailableSlots(Integer providerId, Integer serviceId, LocalDate date) {
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM availability_slots s WHERE s.is_booked = FALSE AND s.slot_date >= CURRENT_DATE");
        appendFilters(sql, parameters, providerId, serviceId, date);
        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, parameters.toArray());
    }

    public List<FilterOption> findProviderOptions() {
        return jdbcTemplate.query("SELECT provider_id AS id, full_name AS name FROM providers ORDER BY full_name",
                (rs, rowNum) -> new FilterOption(rs.getInt("id"), rs.getString("name")));
    }

    public List<FilterOption> findServiceOptions() {
        return jdbcTemplate.query("SELECT service_id AS id, name FROM services ORDER BY name",
                (rs, rowNum) -> new FilterOption(rs.getInt("id"), rs.getString("name")));
    }

    private void appendFilters(StringBuilder sql, List<Object> parameters,
                               Integer providerId, Integer serviceId, LocalDate date) {
        if (providerId != null) {
            sql.append(" AND s.provider_id = ?");
            parameters.add(providerId);
        }
        if (serviceId != null) {
            sql.append(" AND s.service_id = ?");
            parameters.add(serviceId);
        }
        if (date != null) {
            sql.append(" AND s.slot_date = ?");
            parameters.add(Date.valueOf(date));
        }
    }
}
