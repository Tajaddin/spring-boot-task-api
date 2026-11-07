package com.tajaddin.taskapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.tajaddin.taskapi.SpringBootTaskApiApplication;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Boots the real application (default profile, no test profile) on a private
 * H2 database to check which JWT secrets startup accepts.
 */
class JwtSecretStartupTest {

    private static final String DEV_SECRET =
            "dGhpcy1pcy1hLWRldmVsb3BtZW50LW9ubHktc2VjcmV0LWNoYW5nZS1tZQ==";

    @BeforeAll
    static void requireNoSecretInEnvironment() {
        assumeTrue(System.getenv("APP_JWT_SECRET") == null,
                "APP_JWT_SECRET is set in this environment");
    }

    private static ConfigurableApplicationContext start(String... extraArgs) {
        List<String> args = new ArrayList<>(List.of(
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
                "--spring.datasource.username=sa",
                "--spring.datasource.password=",
                "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"));
        args.addAll(List.of(extraArgs));
        return SpringApplication.run(SpringBootTaskApiApplication.class, args.toArray(String[]::new));
    }

    @Test
    void startupFailsWithoutSecret() {
        assertThatThrownBy(JwtSecretStartupTest::start)
                .hasStackTraceContaining("APP_JWT_SECRET");
    }

    @Test
    void startupRejectsDevSecretOutsideDemoAndTestProfiles() {
        assertThatThrownBy(() -> start("--app.jwt.secret=" + DEV_SECRET))
                .hasStackTraceContaining("committed dev secret");
    }

    @Test
    void startupRejectsDevSecretWhenProdProfileIsActive() {
        assertThatThrownBy(() -> start(
                        "--spring.profiles.active=demo,prod", "--spring.datasource.password=real"))
                .hasStackTraceContaining("committed dev secret");
    }

    @Test
    void demoProfileStartsWithItsDevSecret() {
        try (ConfigurableApplicationContext context = start("--spring.profiles.active=demo")) {
            assertThat(context.isRunning()).isTrue();
        }
    }
}
