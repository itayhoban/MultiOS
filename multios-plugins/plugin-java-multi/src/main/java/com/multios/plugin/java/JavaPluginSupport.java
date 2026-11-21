package com.multios.plugin.java;

import com.multios.api.AppEntry;
import com.multios.api.ProcessLaunchPlan;
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
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Shared helpers for Java multi-version plugins.
 */
final class JavaPluginSupport {

    static final int MAX_SCAN_DEPTH = 3;

    private JavaPluginSupport() {
    }

    static List<Path> findJarCandidates(Path appFolder) {
        Objects.requireNonNull(appFolder, "appFolder must not be null");
        if (!Files.isDirectory(appFolder)) {
            return List.of();
        }

        List<Path> candidates = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(appFolder, MAX_SCAN_DEPTH)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .forEach(candidates::add);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan for .jar files under " + appFolder, e);
        }

        candidates.sort(Comparator.comparing(path -> path.toString().toLowerCase(Locale.ROOT)));
        return List.copyOf(candidates);
    }

    static Path vaultJavaExe(Path runtimeVaultDir, String javaFolderName) {
        return runtimeVaultDir.resolve(javaFolderName).resolve("bin").resolve("java.exe");
    }

    static Optional<Path> hostJavaExe() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        Path candidate = javaHome.resolve("bin").resolve("java.exe");
        if (Files.isRegularFile(candidate)) {
            return Optional.of(candidate);
        }
        Path unixCandidate = javaHome.resolve("bin").resolve("java");
        if (Files.isRegularFile(unixCandidate) || Files.isExecutable(unixCandidate)) {
            return Optional.of(unixCandidate);
        }
        return Optional.empty();
    }

    static RuntimeStatus checkVaultJava(Path runtimeVaultDir, String javaFolderName, String label) {
        Path javaExe = vaultJavaExe(runtimeVaultDir, javaFolderName);
        if (Files.isRegularFile(javaExe)) {
            return RuntimeStatus.available(label + " runtime found at " + javaExe);
        }
        return RuntimeStatus.missing(label + " runtime missing. Expected: " + javaExe);
    }

    static RuntimeStatus checkVaultJavaOrHostFallback(Path runtimeVaultDir, String javaFolderName, String label) {
        Path vaultJava = vaultJavaExe(runtimeVaultDir, javaFolderName);
        if (Files.isRegularFile(vaultJava)) {
            return RuntimeStatus.available(label + " runtime found at " + vaultJava);
        }
        Optional<Path> hostJava = hostJavaExe();
        if (hostJava.isPresent()) {
            return RuntimeStatus.available(
                    label + " vault runtime missing; falling back to host JVM at " + hostJava.get());
        }
        return RuntimeStatus.missing(
                label + " runtime missing. Expected vault path " + vaultJava
                        + " and no usable host java.exe was found.");
    }

    static Path resolveJavaExe(Path runtimeVaultDir, String javaFolderName, boolean allowHostFallback) {
        Path vaultJava = vaultJavaExe(runtimeVaultDir, javaFolderName);
        if (Files.isRegularFile(vaultJava)) {
            return vaultJava;
        }
        if (allowHostFallback) {
            return hostJavaExe().orElseThrow(() ->
                    new IllegalStateException(
                            "Java runtime not found in vault (" + vaultJava + ") and host JVM java.exe is unavailable"));
        }
        throw new IllegalStateException("Java runtime not found: " + vaultJava);
    }

    static ProcessLaunchPlan buildJarLaunchPlan(AppEntry app, Path javaExe) {
        Objects.requireNonNull(app, "app must not be null");
        Objects.requireNonNull(javaExe, "javaExe must not be null");

        Path workingDirectory = app.entryPointPath().getParent();
        if (workingDirectory == null) {
            workingDirectory = app.folderPath();
        }

        return new ProcessLaunchPlan(
                List.of(javaExe.toString(), "-jar", app.entryPointPath().toString()),
                workingDirectory,
                Map.of()
        );
    }
}
