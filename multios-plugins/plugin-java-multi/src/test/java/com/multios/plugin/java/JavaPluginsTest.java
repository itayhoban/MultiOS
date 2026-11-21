package com.multios.plugin.java;

import com.multios.api.AppEntry;
import com.multios.api.PlatformType;
import com.multios.api.ProcessLaunchPlan;
import com.multios.api.RuntimeStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaPluginsTest {

    @TempDir
    Path tempDir;

    private final Java8Plugin java8Plugin = new Java8Plugin();
    private final Java21Plugin java21Plugin = new Java21Plugin();

    @Test
    void pluginsReportCorrectPlatforms() {
        assertEquals(PlatformType.JAVA_8, java8Plugin.getSupportedPlatform());
        assertEquals(PlatformType.JAVA_21, java21Plugin.getSupportedPlatform());
    }

    @Test
    void findEntryPointCandidatesFindsJarsWithinDepthLimit() throws Exception {
        Path root = tempDir.resolve("app");
        Path lib = root.resolve("lib");
        Files.createDirectories(lib);
        Path jar = lib.resolve("app.jar");
        Path ignored = lib.resolve("notes.txt");
        Path tooDeep = lib.resolve("a").resolve("b").resolve("c");
        Files.createDirectories(tooDeep);
        Path deepJar = tooDeep.resolve("hidden.jar");

        Files.writeString(jar, "fake");
        Files.writeString(ignored, "text");
        Files.writeString(deepJar, "fake");

        List<Path> candidates = java21Plugin.findEntryPointCandidates(root);

        assertTrue(candidates.contains(jar));
        assertFalse(candidates.contains(deepJar));
        assertEquals(1, candidates.size());
        assertEquals(candidates, java8Plugin.findEntryPointCandidates(root));
    }

    @Test
    void java8AvailabilityRequiresVaultRuntime() throws Exception {
        Path vault = tempDir.resolve("vault");
        Files.createDirectories(vault);

        RuntimeStatus missing = java8Plugin.checkRuntimeAvailability(vault);
        assertFalse(missing.isAvailable());

        Path javaExe = vault.resolve("java-8").resolve("bin").resolve("java.exe");
        Files.createDirectories(javaExe.getParent());
        Files.writeString(javaExe, "fake-java");

        RuntimeStatus available = java8Plugin.checkRuntimeAvailability(vault);
        assertTrue(available.isAvailable());
    }

    @Test
    void java21FallsBackToHostJvmWhenVaultMissing() {
        Path vault = tempDir.resolve("empty-vault");
        RuntimeStatus status = java21Plugin.checkRuntimeAvailability(vault);
        assertTrue(status.isAvailable());
        assertTrue(status.messageOptional().orElse("").toLowerCase().contains("fallback")
                || status.messageOptional().orElse("").contains("host"));
    }

    @Test
    void java21BuildLaunchPlanUsesHostJavaWhenVaultMissing() {
        Path folder = tempDir.resolve("java-app");
        Path jar = folder.resolve("demo.jar");
        AppEntry app = new AppEntry(
                "j21",
                "Demo",
                folder,
                jar,
                PlatformType.JAVA_21,
                Map.of()
        );

        ProcessLaunchPlan plan = java21Plugin.buildLaunchPlan(app, tempDir.resolve("vault"));

        assertEquals(3, plan.command().size());
        assertTrue(plan.command().get(0).toLowerCase().contains("java"));
        assertEquals("-jar", plan.command().get(1));
        assertEquals(jar.toString(), plan.command().get(2));
        assertEquals(folder, plan.workingDirectory());
    }

    @Test
    void java8BuildLaunchPlanUsesVaultJava() throws Exception {
        Path vault = tempDir.resolve("vault");
        Path javaExe = vault.resolve("java-8").resolve("bin").resolve("java.exe");
        Files.createDirectories(javaExe.getParent());
        Files.writeString(javaExe, "fake-java");

        Path folder = tempDir.resolve("legacy-app");
        Path jar = folder.resolve("legacy.jar");
        AppEntry app = new AppEntry(
                "j8",
                "Legacy",
                folder,
                jar,
                PlatformType.JAVA_8,
                Map.of()
        );

        ProcessLaunchPlan plan = java8Plugin.buildLaunchPlan(app, vault);

        assertEquals(List.of(javaExe.toString(), "-jar", jar.toString()), plan.command());
        assertEquals(folder, plan.workingDirectory());
    }

    @Test
    void java8BuildLaunchPlanFailsWhenRuntimeMissing() {
        Path folder = tempDir.resolve("legacy-app");
        Path jar = folder.resolve("legacy.jar");
        AppEntry app = new AppEntry(
                "j8",
                "Legacy",
                folder,
                jar,
                PlatformType.JAVA_8,
                Map.of()
        );

        assertThrows(IllegalStateException.class,
                () -> java8Plugin.buildLaunchPlan(app, tempDir.resolve("missing-vault")));
    }
}
