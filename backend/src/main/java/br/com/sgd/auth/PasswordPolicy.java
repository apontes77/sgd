package br.com.sgd.auth;

import java.nio.charset.StandardCharsets;

/** Central policy for passwords accepted by the local authentication mechanism. */
public final class PasswordPolicy {
    public static final int MIN_BYTES = 12;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() { }

    public static void validate(String password) {
        int bytes = password == null ? 0 : password.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < MIN_BYTES || bytes > MAX_BYTES) throw new InvalidPasswordException();
    }

    public static class InvalidPasswordException extends RuntimeException { }
}
