package com.gameon.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlainTextPasswordEncoderTest {

    private final PlainTextPasswordEncoder encoder = new PlainTextPasswordEncoder();

    @Test
    void storesPasswordExactlyAsEntered() {
        assertThat(encoder.encode("password123")).isEqualTo("password123");
    }

    @Test
    void matchesOnlyTheExactStoredPassword() {
        assertThat(encoder.matches("password123", "password123")).isTrue();
        assertThat(encoder.matches("Password123", "password123")).isFalse();
    }
}
