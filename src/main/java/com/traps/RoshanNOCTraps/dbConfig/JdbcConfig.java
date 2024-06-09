package com.traps.RoshanNOCTraps.dbConfig;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class JdbcConfig {

    @Bean
    JdbcTemplate jdbcTemplate(HikariDataSource hikariDataSource){
        return new JdbcTemplate(hikariDataSource);
    }
}
