package com.gameon.security;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password comparison required by the approved GameOn specification.
 * Passwords are deliberately stored and compared as plain text.
 */
public class PlainTextPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        return rawPassword == null ? null : rawPassword.toString();
    }

    @Override
    public boolean matches(CharSequence rawPassword, String storedPassword) {
        return rawPassword != null && storedPassword != null
                && storedPassword.contentEquals(rawPassword);
    }
}
