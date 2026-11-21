package com.multios.core;

import java.nio.file.Path;
import java.util.Objects;

/**
 * User preferences persisted under {@code %USERPROFILE%/.multios/settings.json}.
 *
 * @param runtimeVaultDir directory where heavy portable runtimes/emulators are stored
 */
public record UserSettings(Path runtimeVaultDir) {

    public UserSettings {
        Objects.requireNonNull(runtimeVaultDir, "runtimeVaultDir must not be null");
    }

    public static UserSettings defaults() {
        return new UserSettings(MultiOsEngine.defaultRuntimeVaultDir());
    }
}
