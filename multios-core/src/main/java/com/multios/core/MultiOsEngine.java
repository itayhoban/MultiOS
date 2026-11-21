package com.multios.core;

import com.multios.api.AppEntry;
import com.multios.api.PlatformType;
import com.multios.api.ProcessLaunchPlan;
import com.multios.api.RuntimePlugin;
import com.multios.api.RuntimeStatus;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Facade connecting the app library, plugin registry, runtime vault, and process supervisor.
 */
public final class MultiOsEngine implements AutoCloseable {

    private final AppLibraryRepository appLibrary;
    private final PluginRegistry pluginRegistry;
    private final Path runtimeVaultDir;
    private final ProcessSupervisor processSupervisor;

    public MultiOsEngine() {
        this(
                new AppLibraryRepository(),
                new PluginRegistry(),
                defaultRuntimeVaultDir(),
                new ProcessSupervisor()
        );
    }

    public MultiOsEngine(Path configDirectory, Path runtimeVaultDir) {
        this(
                new AppLibraryRepository(configDirectory),
                new PluginRegistry(),
                runtimeVaultDir,
                new ProcessSupervisor()
        );
    }

    public MultiOsEngine(
            AppLibraryRepository appLibrary,
            PluginRegistry pluginRegistry,
            Path runtimeVaultDir,
            ProcessSupervisor processSupervisor
    ) {
        this.appLibrary = Objects.requireNonNull(appLibrary, "appLibrary must not be null");
        this.pluginRegistry = Objects.requireNonNull(pluginRegistry, "pluginRegistry must not be null");
        this.runtimeVaultDir = Objects.requireNonNull(runtimeVaultDir, "runtimeVaultDir must not be null");
        this.processSupervisor = Objects.requireNonNull(processSupervisor, "processSupervisor must not be null");
    }

    public static Path defaultRuntimeVaultDir() {
        return Path.of(System.getProperty("user.home"), ".multios", "runtimes");
    }

    public AppLibraryRepository getAppLibrary() {
        return appLibrary;
    }

    public PluginRegistry getPluginRegistry() {
        return pluginRegistry;
    }

    public Path getRuntimeVaultDir() {
        return runtimeVaultDir;
    }

    public List<AppEntry> listApps() {
        return appLibrary.listApps();
    }

    public void saveApp(AppEntry entry) {
        appLibrary.save(entry);
    }

    public boolean deleteApp(String id) {
        return appLibrary.delete(id);
    }

    public Optional<RuntimePlugin> findPlugin(PlatformType platformType) {
        return pluginRegistry.findByPlatform(platformType);
    }

    public List<Path> findEntryPointCandidates(PlatformType platformType, Path appFolder) {
        return pluginRegistry.requireByPlatform(platformType).findEntryPointCandidates(appFolder);
    }

    public RuntimeStatus checkRuntimeAvailability(PlatformType platformType) {
        return pluginRegistry.requireByPlatform(platformType).checkRuntimeAvailability(runtimeVaultDir);
    }

    public ProcessLaunchPlan buildLaunchPlan(AppEntry app) {
        Objects.requireNonNull(app, "app must not be null");
        return pluginRegistry.requireByPlatform(app.platformType()).buildLaunchPlan(app, runtimeVaultDir);
    }

    public ProcessSession launch(AppEntry app) {
        return launch(app, (stream, line) -> {
        });
    }

    public ProcessSession launch(AppEntry app, ProcessOutputListener listener) {
        ProcessLaunchPlan plan = buildLaunchPlan(app);
        return processSupervisor.start(plan, listener);
    }

    @Override
    public void close() {
        processSupervisor.close();
    }
}
