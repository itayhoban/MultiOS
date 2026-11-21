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
 * Launches JAR applications using a portable Java 8 runtime from the Runtime Vault.
 */
public final class Java8Plugin implements RuntimePlugin {

    static final String VAULT_FOLDER = "java-8";

    @Override
    public PlatformType getSupportedPlatform() {
        return PlatformType.JAVA_8;
    }

    @Override
    public List<Path> findEntryPointCandidates(Path appFolder) {
        return JavaPluginSupport.findJarCandidates(appFolder);
    }

    @Override
    public RuntimeStatus checkRuntimeAvailability(Path runtimeVaultDir) {
        Objects.requireNonNull(runtimeVaultDir, "runtimeVaultDir must not be null");
        return JavaPluginSupport.checkVaultJava(runtimeVaultDir, VAULT_FOLDER, "Java 8");
    }

    @Override
    public ProcessLaunchPlan buildLaunchPlan(AppEntry app, Path runtimeVaultDir) {
        Objects.requireNonNull(app, "app must not be null");
        Objects.requireNonNull(runtimeVaultDir, "runtimeVaultDir must not be null");
        Path javaExe = JavaPluginSupport.resolveJavaExe(runtimeVaultDir, VAULT_FOLDER, false);
        return JavaPluginSupport.buildJarLaunchPlan(app, javaExe);
    }
}
