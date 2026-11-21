package com.multios.api;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Persisted software item configured by the user.
 *
 * @param id              unique identifier
 * @param displayName     human-readable name shown in the UI
 * @param folderPath      root folder containing the software files
 * @param entryPointPath  resolved main launch file within the folder
 * @param platformType    target platform selected by the user
 * @param metadata        optional platform-specific key/value metadata
 */
public record AppEntry(
        String id,
        String displayName,
        Path folderPath,
        Path entryPointPath,
        PlatformType platformType,
        Map<String, String> metadata
) {
    public AppEntry {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(folderPath, "folderPath must not be null");
        Objects.requireNonNull(entryPointPath, "entryPointPath must not be null");
        Objects.requireNonNull(platformType, "platformType must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
