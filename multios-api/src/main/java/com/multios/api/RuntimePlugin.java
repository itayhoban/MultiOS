package com.multios.api;

import java.nio.file.Path;
import java.util.List;

/**
 * Platform-specific runner contract. Implementations live in plugin modules
 * and are discovered via {@link java.util.ServiceLoader}.
 */
public interface RuntimePlugin {

    /**
     * @return the single platform this plugin handles
     */
    PlatformType getSupportedPlatform();

    /**
     * Scans an application folder for candidate launch files for this platform.
     *
     * @param appFolder root folder selected by the user
     * @return ordered list of candidate entry-point paths (may be empty)
     */
    List<Path> findEntryPointCandidates(Path appFolder);

    /**
     * Checks whether the required runtime/emulator exists under the Runtime Vault.
     *
     * @param runtimeVaultDir root of {@code .multios/runtimes/}
     * @return availability status with an optional detail message
     */
    RuntimeStatus checkRuntimeAvailability(Path runtimeVaultDir);

    /**
     * Builds an explicit launch plan for the given app using vault-local runtimes only.
     *
     * @param app              configured application entry
     * @param runtimeVaultDir  root of {@code .multios/runtimes/}
     * @return process launch plan for {@link ProcessBuilder}
     */
    ProcessLaunchPlan buildLaunchPlan(AppEntry app, Path runtimeVaultDir);
}
