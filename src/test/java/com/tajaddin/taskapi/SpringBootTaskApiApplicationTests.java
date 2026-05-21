package com.tajaddin.taskapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SpringBootTaskApiApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the full Spring context boots with H2 + Flyway migrations.
    }
}
