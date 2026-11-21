package com.multios.plugin.win;

import com.multios.api.AppEntry;
import com.multios.api.PlatformType;
import com.multios.api.ProcessLaunchPlan;
import com.multios.api.RuntimeStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsNativePluginTest {

    @TempDir
    Path tempDir;

    private final WindowsNativePlugin plugin = new WindowsNativePlugin();

    @Test
    void supportedPlatformIsWindowsNative() {
        assertEquals(PlatformType.WINDOWS_NATIVE, plugin.getSupportedPlatform());
    }

    @Test
    void findEntryPointCandidatesFindsExeWithinDepthLimit() throws Exception {
        Path root = tempDir.resolve("app");
        Path nested = root.resolve("bin");
        Files.createDirectories(nested);
        Path exe = nested.resolve("game.exe");
        Path ignoredTxt = nested.resolve("readme.txt");
        Path tooDeep = nested.resolve("a").resolve("b").resolve("c");
        Files.createDirectories(tooDeep);
        Path deepExe = tooDeep.resolve("hidden.exe");

        Files.writeString(exe, "fake");
        Files.writeString(ignoredTxt, "text");
        Files.writeString(deepExe, "fake");

        List<Path> candidates = plugin.findEntryPointCandidates(root);

        assertTrue(candidates.contains(exe));
        assertFalse(candidates.contains(deepExe));
        assertEquals(1, candidates.size());
    }

    @Test
    void checkRuntimeAvailabilityReflectsWindowsHost() {
        RuntimeStatus status = plugin.checkRuntimeAvailability(tempDir.resolve("vault"));
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            assertTrue(status.isAvailable());
        } else {
            assertFalse(status.isAvailable());
        }
    }

    @Test
    void buildLaunchPlanUsesExeAndParentAsWorkingDirectory() {
        Path folder = tempDir.resolve("Game");
        Path entry = folder.resolve("play.exe");
        AppEntry app = new AppEntry(
                "win-1",
                "Game",
                folder,
                entry,
                PlatformType.WINDOWS_NATIVE,
                Map.of()
        );

        ProcessLaunchPlan plan = plugin.buildLaunchPlan(app, tempDir.resolve("vault"));

        assertEquals(List.of(entry.toString()), plan.command());
        assertEquals(folder, plan.workingDirectory());
    }
}
