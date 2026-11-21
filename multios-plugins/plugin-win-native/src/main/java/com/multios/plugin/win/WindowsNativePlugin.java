package com.multios.plugin.win;

import com.multios.api.AppEntry;
import com.multios.api.PlatformType;
import com.multios.api.ProcessLaunchPlan;
import com.multios.api.RuntimePlugin;
import com.multios.api.RuntimeStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Launches standard modern Windows native executables (.exe).
 */
public final class WindowsNativePlugin implements RuntimePlugin {

    static final int MAX_SCAN_DEPTH = 3;

    @Override
    public PlatformType getSupportedPlatform() {
        return PlatformType.WINDOWS_NATIVE;
    }

    @Override
    public List<Path> findEntryPointCandidates(Path appFolder) {
        Objects.requireNonNull(appFolder, "appFolder must not be null");
        if (!Files.isDirectory(appFolder)) {
            return List.of();
        }

        List<Path> candidates = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(appFolder, MAX_SCAN_DEPTH)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe"))
                    .forEach(candidates::add);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan for .exe files under " + appFolder, e);
        }

        candidates.sort(Comparator.comparing(path -> path.toString().toLowerCase(Locale.ROOT)));
        return List.copyOf(candidates);
    }

    @Override
    public RuntimeStatus checkRuntimeAvailability(Path runtimeVaultDir) {
        Objects.requireNonNull(runtimeVaultDir, "runtimeVaultDir must not be null");
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            return RuntimeStatus.available("Host OS is Windows; native executables can be launched directly.");
        }
        return RuntimeStatus.missing("Windows Native apps require a Windows host OS. Detected: " + osName);
    }

    @Override
    public ProcessLaunchPlan buildLaunchPlan(AppEntry app, Path runtimeVaultDir) {
        Objects.requireNonNull(app, "app must not be null");
        Objects.requireNonNull(runtimeVaultDir, "runtimeVaultDir must not be null");

        Path entryPoint = app.entryPointPath();
        Path workingDirectory = entryPoint.getParent();
        if (workingDirectory == null) {
            workingDirectory = app.folderPath();
        }

        return new ProcessLaunchPlan(
                List.of(entryPoint.toString()),
                workingDirectory,
                Map.of()
        );
    }
}
