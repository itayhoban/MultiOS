package com.multios.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multios.api.AppEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * JSON-backed persistence for the user's configured application library.
 * Default storage location: {@code %USERPROFILE%/.multios/apps.json}.
 */
public final class AppLibraryRepository {

    public static final String DEFAULT_APPS_FILE_NAME = "apps.json";

    private final Path appsFile;
    private final ObjectMapper objectMapper;
    private final List<AppEntry> apps;

    public AppLibraryRepository() {
        this(defaultConfigDirectory());
    }

    public AppLibraryRepository(Path configDirectory) {
        this(configDirectory, JacksonConfig.createObjectMapper());
    }

    AppLibraryRepository(Path configDirectory, ObjectMapper objectMapper) {
        Objects.requireNonNull(configDirectory, "configDirectory must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.appsFile = configDirectory.resolve(DEFAULT_APPS_FILE_NAME);
        this.apps = new CopyOnWriteArrayList<>();
        loadFromDisk();
    }

    public static Path defaultConfigDirectory() {
        return Path.of(System.getProperty("user.home"), ".multios");
    }

    public Path getAppsFile() {
        return appsFile;
    }

    public List<AppEntry> listApps() {
        return List.copyOf(apps);
    }

    public Optional<AppEntry> findById(String id) {
        Objects.requireNonNull(id, "id must not be null");
        return apps.stream().filter(app -> app.id().equals(id)).findFirst();
    }

    public void save(AppEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        apps.removeIf(existing -> existing.id().equals(entry.id()));
        apps.add(entry);
        persist();
    }

    public boolean delete(String id) {
        Objects.requireNonNull(id, "id must not be null");
        boolean removed = apps.removeIf(existing -> existing.id().equals(id));
        if (removed) {
            persist();
        }
        return removed;
    }

    public void replaceAll(List<AppEntry> entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        apps.clear();
        apps.addAll(entries);
        persist();
    }

    private void loadFromDisk() {
        if (!Files.isRegularFile(appsFile)) {
            return;
        }
        try {
            List<AppEntry> loaded = objectMapper.readValue(appsFile.toFile(), new TypeReference<>() {
            });
            apps.clear();
            if (loaded != null) {
                apps.addAll(loaded);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load app library from " + appsFile, e);
        }
    }

    private void persist() {
        try {
            Path parent = appsFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writeValue(appsFile.toFile(), new ArrayList<>(apps));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save app library to " + appsFile, e);
        }
    }
}
