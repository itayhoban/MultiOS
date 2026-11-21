package com.multios.plugin.java;

import com.multios.api.AppEntry;
import com.multios.api.PlatformType;
import com.multios.api.ProcessLaunchPlan;
import com.multios.api.RuntimePlugin;
import com.multios.api.RuntimeStatus;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Launches JAR applications using a portable Java 21 runtime from the Runtime Vault,
 * with fallback to the current host JVM so it works out of the box.
 */
public final class Java21Plugin implements RuntimePlugin {

    static final String VAULT_FOLDER = "java-21";

    @Override
    public PlatformType getSupportedPlatform() {
        return PlatformType.JAVA_21;
    }

    @Override
    public List<Path> findEntryPointCandidates(Path appFolder) {
        return JavaPluginSupport.findJarCandidates(appFolder);
    }

    @Override
    public RuntimeStatus checkRuntimeAvailability(Path runtimeVaultDir) {
        Objects.requireNonNull(runtimeVaultDir, "runtimeVaultDir must not be null");
        return JavaPluginSupport.checkVaultJavaOrHostFallback(runtimeVaultDir, VAULT_FOLDER, "Java 21");
    }

    @Override
    public ProcessLaunchPlan buildLaunchPlan(AppEntry app, Path runtimeVaultDir) {
        Objects.requireNonNull(app, "app must not be null");
        Objects.requireNonNull(runtimeVaultDir, "runtimeVaultDir must not be null");
        Path javaExe = JavaPluginSupport.resolveJavaExe(runtimeVaultDir, VAULT_FOLDER, true);
        return JavaPluginSupport.buildJarLaunchPlan(app, javaExe);
    }
}
