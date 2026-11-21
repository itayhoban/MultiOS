package com.multios.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of checking whether a required runtime/emulator is present in the Runtime Vault.
 *
 * @param availability whether the runtime is available or missing
 * @param message      optional human-readable detail (e.g. what is missing)
 */
public record RuntimeStatus(Availability availability, String message) {

    public enum Availability {
        AVAILABLE,
        MISSING
    }

    public RuntimeStatus {
        Objects.requireNonNull(availability, "availability must not be null");
    }

    public Optional<String> messageOptional() {
        return Optional.ofNullable(message);
    }

    public boolean isAvailable() {
        return availability == Availability.AVAILABLE;
    }

    public static RuntimeStatus available() {
        return new RuntimeStatus(Availability.AVAILABLE, null);
    }

    public static RuntimeStatus available(String message) {
        return new RuntimeStatus(Availability.AVAILABLE, message);
    }

    public static RuntimeStatus missing(String message) {
        return new RuntimeStatus(Availability.MISSING, message);
    }
}
