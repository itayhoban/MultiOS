package com.multios.core;

import com.multios.api.AppEntry;
import com.multios.api.PlatformType;
import com.multios.api.ProcessLaunchPlan;
import com.multios.api.RuntimePlugin;
import com.multios.api.RuntimeStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginRegistryTest {

    @Test
    void findsPluginByPlatform() {
        RuntimePlugin windows = stubPlugin(PlatformType.WINDOWS_NATIVE);
        RuntimePlugin java21 = stubPlugin(PlatformType.JAVA_21);

        PluginRegistry registry = new PluginRegistry(List.of(windows, java21));

        assertTrue(registry.findByPlatform(PlatformType.WINDOWS_NATIVE).isPresent());
        assertEquals(windows, registry.requireByPlatform(PlatformType.WINDOWS_NATIVE));
        assertEquals(java21, registry.requireByPlatform(PlatformType.JAVA_21));
        assertEquals(2, registry.allPlugins().size());
    }

    @Test
    void rejectsDuplicatePlatformPlugins() {
        RuntimePlugin first = stubPlugin(PlatformType.JAVA_8);
        RuntimePlugin second = stubPlugin(PlatformType.JAVA_8);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new PluginRegistry(List.of(first, second))
        );
        assertTrue(ex.getMessage().contains("JAVA_8"));
    }

    @Test
    void requireByPlatformThrowsWhenMissing() {
        PluginRegistry registry = new PluginRegistry(List.of());
        assertThrows(IllegalArgumentException.class, () -> registry.requireByPlatform(PlatformType.LINUX));
    }

    private static RuntimePlugin stubPlugin(PlatformType platformType) {
        return new RuntimePlugin() {
            @Override
            public PlatformType getSupportedPlatform() {
                return platformType;
            }

            @Override
            public List<Path> findEntryPointCandidates(Path appFolder) {
                return List.of();
            }

            @Override
            public RuntimeStatus checkRuntimeAvailability(Path runtimeVaultDir) {
                return RuntimeStatus.available();
            }

            @Override
            public ProcessLaunchPlan buildLaunchPlan(AppEntry app, Path runtimeVaultDir) {
                return new ProcessLaunchPlan(List.of("noop"), Path.of("."), Map.of());
            }
        };
    }
}
