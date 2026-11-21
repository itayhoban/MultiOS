package com.multios.core;

import com.multios.api.AppEntry;
import com.multios.api.PlatformType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppLibraryRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndReloadPersistsAppsAsJson() throws Exception {
        Path configDir = tempDir.resolve("config");
        AppLibraryRepository repository = new AppLibraryRepository(configDir);

        AppEntry entry = new AppEntry(
                "app-1",
                "Demo App",
                Path.of("C:", "Games", "Demo"),
                Path.of("C:", "Games", "Demo", "demo.exe"),
                PlatformType.WINDOWS_NATIVE,
                Map.of("note", "test")
        );
        repository.save(entry);

        Path appsFile = configDir.resolve("apps.json");
        assertTrue(Files.isRegularFile(appsFile));
        String json = Files.readString(appsFile);
        assertTrue(json.contains("Demo App"));
        assertTrue(json.contains("WINDOWS_NATIVE"));

        AppLibraryRepository reloaded = new AppLibraryRepository(configDir);
        List<AppEntry> apps = reloaded.listApps();
        assertEquals(1, apps.size());
        assertEquals("app-1", apps.getFirst().id());
        assertEquals("Demo App", apps.getFirst().displayName());
        assertEquals(PlatformType.WINDOWS_NATIVE, apps.getFirst().platformType());
        assertEquals("test", apps.getFirst().metadata().get("note"));
    }

    @Test
    void saveReplacesExistingEntryWithSameId() {
        AppLibraryRepository repository = new AppLibraryRepository(tempDir.resolve("config"));

        repository.save(new AppEntry(
                "same-id",
                "Old Name",
                Path.of("C:", "old"),
                Path.of("C:", "old", "a.exe"),
                PlatformType.WINDOWS_NATIVE,
                Map.of()
        ));
        repository.save(new AppEntry(
                "same-id",
                "New Name",
                Path.of("C:", "new"),
                Path.of("C:", "new", "b.exe"),
                PlatformType.JAVA_21,
                Map.of()
        ));

        List<AppEntry> apps = repository.listApps();
        assertEquals(1, apps.size());
        assertEquals("New Name", apps.getFirst().displayName());
        assertEquals(PlatformType.JAVA_21, apps.getFirst().platformType());
    }

    @Test
    void deleteRemovesEntry() {
        AppLibraryRepository repository = new AppLibraryRepository(tempDir.resolve("config"));
        repository.save(new AppEntry(
                "to-delete",
                "Gone",
                Path.of("C:", "x"),
                Path.of("C:", "x", "x.exe"),
                PlatformType.WINDOWS_NATIVE,
                Map.of()
        ));

        assertTrue(repository.delete("to-delete"));
        assertFalse(repository.findById("to-delete").isPresent());
        assertTrue(repository.listApps().isEmpty());
    }
}
