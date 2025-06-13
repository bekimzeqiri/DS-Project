package com.leaderboard.discovery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Eureka Server
 *
 * This configuration secures the Eureka dashboard while allowing
 * service registration and discovery endpoints to be accessible.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        // Allow access to Eureka endpoints for service registration
                        .requestMatchers("/eureka/**").permitAll()
                        // Allow access to actuator endpoints
                        .requestMatchers("/actuator/**").permitAll()
                        // Require authentication for Eureka dashboard
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> {});

        return http.build();
    }
}