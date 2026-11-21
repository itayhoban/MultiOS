package com.multios.core;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Loads and saves {@link UserSettings} as JSON.
 * Default location: {@code %USERPROFILE%/.multios/settings.json}.
 */
public final class UserSettingsStore {

    public static final String DEFAULT_SETTINGS_FILE_NAME = "settings.json";

    private final Path settingsFile;
    private final ObjectMapper objectMapper;

    public UserSettingsStore() {
        this(AppLibraryRepository.defaultConfigDirectory());
    }

    public UserSettingsStore(Path configDirectory) {
        this(configDirectory, JacksonConfig.createObjectMapper());
    }

    UserSettingsStore(Path configDirectory, ObjectMapper objectMapper) {
        Objects.requireNonNull(configDirectory, "configDirectory must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.settingsFile = configDirectory.resolve(DEFAULT_SETTINGS_FILE_NAME);
    }

    public Path getSettingsFile() {
        return settingsFile;
    }

    public UserSettings loadOrDefault() {
        if (!Files.isRegularFile(settingsFile)) {
            return UserSettings.defaults();
        }
        try {
            UserSettings loaded = objectMapper.readValue(settingsFile.toFile(), UserSettings.class);
            return loaded != null ? loaded : UserSettings.defaults();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load user settings from " + settingsFile, e);
        }
    }

    public void save(UserSettings settings) {
        Objects.requireNonNull(settings, "settings must not be null");
        try {
            Path parent = settingsFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writeValue(settingsFile.toFile(), settings);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save user settings to " + settingsFile, e);
        }
    }
}
