package com.cmpe172.fitness.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads the account and its authority from the PostgreSQL account row. */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String sql = """
                SELECT email, password, role FROM users WHERE LOWER(email) = LOWER(?)
                UNION ALL
                SELECT email, password, role FROM providers WHERE LOWER(email) = LOWER(?)
                """;

        try {
            Account account = jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                    new Account(rs.getString("email"), rs.getString("password"), rs.getString("role")),
                    email, email);
            return User.withUsername(account.email())
                    .password(account.password())
                    .authorities("ROLE_" + account.role())
                    .build();
        } catch (org.springframework.dao.EmptyResultDataAccessException exception) {
            throw new UsernameNotFoundException("Account not found", exception);
        }
    }

    private record Account(String email, String password, String role) { }
}
