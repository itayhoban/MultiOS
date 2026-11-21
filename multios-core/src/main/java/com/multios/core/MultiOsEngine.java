package com.multios.core;

import com.multios.api.AppEntry;
import com.multios.api.PlatformType;
import com.multios.api.ProcessLaunchPlan;
import com.multios.api.RuntimePlugin;
import com.multios.api.RuntimeStatus;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Facade connecting the app library, plugin registry, runtime vault, and process supervisor.
 */
public final class MultiOsEngine implements AutoCloseable {

    private final AppLibraryRepository appLibrary;
    private final PluginRegistry pluginRegistry;
    private final UserSettingsStore settingsStore;
    private final ProcessSupervisor processSupervisor;
    private final ConcurrentMap<String, ProcessSession> sessionsByAppId;

    private volatile Path runtimeVaultDir;

    public MultiOsEngine() {
        this(
                AppLibraryRepository.defaultConfigDirectory(),
                new UserSettingsStore().loadOrDefault().runtimeVaultDir()
        );
    }

    public MultiOsEngine(Path configDirectory, Path runtimeVaultDir) {
        this(
                new AppLibraryRepository(configDirectory),
                new PluginRegistry(),
                new UserSettingsStore(configDirectory),
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
        this(
                appLibrary,
                pluginRegistry,
                new UserSettingsStore(AppLibraryRepository.defaultConfigDirectory()),
                runtimeVaultDir,
                processSupervisor
        );
    }

    public MultiOsEngine(
            AppLibraryRepository appLibrary,
            PluginRegistry pluginRegistry,
            UserSettingsStore settingsStore,
            Path runtimeVaultDir,
            ProcessSupervisor processSupervisor
    ) {
        this.appLibrary = Objects.requireNonNull(appLibrary, "appLibrary must not be null");
        this.pluginRegistry = Objects.requireNonNull(pluginRegistry, "pluginRegistry must not be null");
        this.settingsStore = Objects.requireNonNull(settingsStore, "settingsStore must not be null");
        this.runtimeVaultDir = Objects.requireNonNull(runtimeVaultDir, "runtimeVaultDir must not be null");
        this.processSupervisor = Objects.requireNonNull(processSupervisor, "processSupervisor must not be null");
        this.sessionsByAppId = new ConcurrentHashMap<>();
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

    public UserSettingsStore getSettingsStore() {
        return settingsStore;
    }

    public Path getRuntimeVaultDir() {
        return runtimeVaultDir;
    }

    /**
     * Updates the Runtime Vault directory and persists it to user settings.
     */
    public void updateRuntimeVaultDir(Path newRuntimeVaultDir) {
        Objects.requireNonNull(newRuntimeVaultDir, "newRuntimeVaultDir must not be null");
        this.runtimeVaultDir = newRuntimeVaultDir.toAbsolutePath().normalize();
        settingsStore.save(new UserSettings(this.runtimeVaultDir));
    }

    public List<AppEntry> listApps() {
        return appLibrary.listApps();
    }

    public void saveApp(AppEntry entry) {
        appLibrary.save(entry);
    }

    public boolean deleteApp(String id) {
        Objects.requireNonNull(id, "id must not be null");
        stop(id);
        sessionsByAppId.remove(id);
        return appLibrary.delete(id);
    }

    public Optional<RuntimePlugin> findPlugin(PlatformType platformType) {
        return pluginRegistry.findByPlatform(platformType);
    }

    /**
     * @return platforms for which a plugin is currently registered, sorted by name
     */
    public List<PlatformType> listRegisteredPlatforms() {
        return pluginRegistry.allPlugins().stream()
                .map(RuntimePlugin::getSupportedPlatform)
                .sorted(Comparator.comparing(Enum::name))
                .toList();
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
        Objects.requireNonNull(app, "app must not be null");
        Objects.requireNonNull(listener, "listener must not be null");

        stop(app.id());

        ProcessLaunchPlan plan = buildLaunchPlan(app);
        ProcessSession session = processSupervisor.start(plan, listener);
        sessionsByAppId.put(app.id(), session);
        return session;
    }

    public Optional<ProcessSession> getSession(String appId) {
        Objects.requireNonNull(appId, "appId must not be null");
        return Optional.ofNullable(sessionsByAppId.get(appId));
    }

    public AppRunStatus getAppRunStatus(String appId) {
        Objects.requireNonNull(appId, "appId must not be null");
        ProcessSession session = sessionsByAppId.get(appId);
        if (session == null) {
            return AppRunStatus.IDLE;
        }
        return switch (session.getStatus()) {
            case RUNNING -> AppRunStatus.RUNNING;
            case EXITED, FAILED -> AppRunStatus.EXITED;
        };
    }

    /**
     * Requests termination of a running session for the given app id (no-op if idle).
     */
    public void stop(String appId) {
        Objects.requireNonNull(appId, "appId must not be null");
        ProcessSession session = sessionsByAppId.get(appId);
        if (session != null && session.getStatus() == ProcessStatus.RUNNING) {
            session.terminate();
        }
    }

    @Override
    public void close() {
        for (ProcessSession session : sessionsByAppId.values()) {
            if (session.getStatus() == ProcessStatus.RUNNING) {
                session.terminate();
            }
        }
        sessionsByAppId.clear();
        processSupervisor.close();
    }
}
