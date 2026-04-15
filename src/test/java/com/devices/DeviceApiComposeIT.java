package com.devices;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Same checks as {@link DeviceApiIT}, but against Postgres published by {@code docker compose}
 * (defaults: host {@code localhost}, port {@code 5433}; override with {@code RUN_COMPOSE_HOST} / {@code RUN_COMPOSE_PORT}).
 * <p>
 * Run with {@code mvn verify -Pcompose-postgres} while Compose is up. Prefer {@code docker compose stop app}
 * so tests and the running API do not share the same database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("compose")
class DeviceApiComposeIT extends DeviceApiTestSupport {

    @DynamicPropertySource
    static void composePostgres(DynamicPropertyRegistry registry) {
        var host = System.getenv().getOrDefault("RUN_COMPOSE_HOST", "localhost");
        var port = System.getenv().getOrDefault("RUN_COMPOSE_PORT", "5433");
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + host + ":" + port + "/devices");
        registry.add("spring.datasource.username", () -> "devices");
        registry.add("spring.datasource.password", () -> "devices");
    }
}
