package com.leaderboard.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway Configuration
 *
 * Defines routing rules for the API Gateway using programmatic configuration.
 * This provides more flexibility than property-based configuration.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Player Service Routes
                .route("player-service", r -> r
                        .path("/api/players/**")
                        .filters(f -> f
                                .stripPrefix(2)  // Remove /api/players from path
                                .addRequestHeader("X-Gateway", "api-gateway")
                                .addResponseHeader("X-Response-Time", "#{T(System).currentTimeMillis()}")
                        )
                        .uri("lb://player-service")
                )

                // Score Service Routes
                .route("score-service", r -> r
                        .path("/api/scores/**")
                        .filters(f -> f
                                .stripPrefix(2)  // Remove /api/scores from path
                                .addRequestHeader("X-Gateway", "api-gateway")
                                .addResponseHeader("X-Response-Time", "#{T(System).currentTimeMillis()}")
                        )
                        .uri("lb://score-service")
                )

                // Leaderboard Service Routes
                .route("leaderboard-service", r -> r
                        .path("/api/leaderboards/**")
                        .filters(f -> f
                                .stripPrefix(2)  // Remove /api/leaderboards from path
                                .addRequestHeader("X-Gateway", "api-gateway")
                                .addResponseHeader("X-Response-Time", "#{T(System).currentTimeMillis()}")
                        )
                        .uri("lb://leaderboard-service")
                )

                // Notification Service Routes
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f
                                .stripPrefix(2)  // Remove /api/notifications from path
                                .addRequestHeader("X-Gateway", "api-gateway")
                                .addResponseHeader("X-Response-Time", "#{T(System).currentTimeMillis()}")
                        )
                        .uri("lb://notification-service")
                )

                // Health Check Route - Direct to actuator endpoints
                .route("health-check", r -> r
                        .path("/health/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .addRequestHeader("X-Health-Check", "gateway")
                        )
                        .uri("http://localhost:8080")
                )

                .build();
    }
}