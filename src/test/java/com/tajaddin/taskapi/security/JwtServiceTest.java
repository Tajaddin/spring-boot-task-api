package com.tajaddin.taskapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    // 256-bit base64 secret for tests.
    private static final String SECRET = "dGhpcy1pcy1hLXRlc3Qtb25seS1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0aW5n";

    private final JwtService jwt = new JwtService(SECRET, 60);

    @Test
    void issuesAndParsesToken() {
        String token = jwt.issue(42L, "user@example.com");
        assertThat(token).isNotBlank();
        assertThat(jwt.parseUserId(token)).isEqualTo(42L);
    }

    @Test
    void ttlSecondsMatchesConfiguredMinutes() {
        assertThat(jwt.getTtlSeconds()).isEqualTo(3600L);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwt.issue(1L, "a@b.com");
        String tampered = token.substring(0, token.length() - 2) + "xy";
        assertThatThrownBy(() -> jwt.parseUserId(tampered)).isInstanceOf(Exception.class);
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        String otherSecret = "YW5vdGhlci10ZXN0LXNlY3JldC1rZXktdGhhdC1pcy0yNTYtYml0cy1sb25n";
        JwtService other = new JwtService(otherSecret, 60);
        String foreignToken = other.issue(7L, "x@y.com");
        assertThatThrownBy(() -> jwt.parseUserId(foreignToken)).isInstanceOf(Exception.class);
    }
}
