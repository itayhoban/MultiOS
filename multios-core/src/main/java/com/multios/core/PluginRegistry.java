package com.multios.core;

import com.multios.api.PlatformType;
import com.multios.api.RuntimePlugin;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Discovers {@link RuntimePlugin} implementations via {@link ServiceLoader}
 * and provides lookup by {@link PlatformType}.
 */
public final class PluginRegistry {

    private final Map<PlatformType, RuntimePlugin> pluginsByPlatform;

    public PluginRegistry() {
        this(ServiceLoader.load(RuntimePlugin.class));
    }

    public PluginRegistry(ClassLoader classLoader) {
        this(ServiceLoader.load(RuntimePlugin.class, classLoader));
    }

    public PluginRegistry(Iterable<RuntimePlugin> plugins) {
        Objects.requireNonNull(plugins, "plugins must not be null");
        Map<PlatformType, RuntimePlugin> map = new EnumMap<>(PlatformType.class);
        for (RuntimePlugin plugin : plugins) {
            PlatformType platform = plugin.getSupportedPlatform();
            RuntimePlugin previous = map.put(platform, plugin);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate RuntimePlugin for platform " + platform
                                + ": " + previous.getClass().getName()
                                + " and " + plugin.getClass().getName());
            }
        }
        this.pluginsByPlatform = Map.copyOf(map);
    }

    public Optional<RuntimePlugin> findByPlatform(PlatformType platformType) {
        Objects.requireNonNull(platformType, "platformType must not be null");
        return Optional.ofNullable(pluginsByPlatform.get(platformType));
    }

    public RuntimePlugin requireByPlatform(PlatformType platformType) {
        return findByPlatform(platformType).orElseThrow(() ->
                new IllegalArgumentException("No RuntimePlugin registered for platform: " + platformType));
    }

    public Collection<RuntimePlugin> allPlugins() {
        return List.copyOf(pluginsByPlatform.values());
    }

    public boolean isEmpty() {
        return pluginsByPlatform.isEmpty();
    }
}
