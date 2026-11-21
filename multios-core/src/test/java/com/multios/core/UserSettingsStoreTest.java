package com.multios.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSettingsStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void loadOrDefaultReturnsDefaultsWhenFileMissing() {
        UserSettingsStore store = new UserSettingsStore(tempDir);
        UserSettings settings = store.loadOrDefault();
        assertEquals(MultiOsEngine.defaultRuntimeVaultDir(), settings.runtimeVaultDir());
    }

    @Test
    void saveAndReloadRoundTripsRuntimeVaultDir() {
        UserSettingsStore store = new UserSettingsStore(tempDir);
        Path vault = tempDir.resolve("vault-on-d");

        store.save(new UserSettings(vault));

        assertTrue(java.nio.file.Files.isRegularFile(store.getSettingsFile()));
        assertEquals(vault, store.loadOrDefault().runtimeVaultDir());
    }
}
