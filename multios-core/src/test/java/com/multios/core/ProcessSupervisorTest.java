package com.multios.core;

import com.multios.api.ProcessLaunchPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessSupervisorTest {

    @TempDir
    Path tempDir;

    @Test
    void startsProcessStreamsStdoutAndTracksExit() throws Exception {
        try (ProcessSupervisor supervisor = new ProcessSupervisor()) {
            List<String> stdoutLines = new CopyOnWriteArrayList<>();
            ProcessLaunchPlan plan = windowsEchoPlan("hello-multios");

            ProcessSession session = supervisor.start(plan, (stream, line) -> {
                if (stream == ProcessOutputStream.STDOUT) {
                    stdoutLines.add(line);
                }
            });

            Integer exitCode = session.onExit().get(15, TimeUnit.SECONDS);
            assertEquals(0, exitCode);
            assertEquals(ProcessStatus.EXITED, session.getStatus());
            assertEquals(0, session.getExitCode().orElseThrow());
            assertTrue(stdoutLines.stream().anyMatch(line -> line.contains("hello-multios")));
        }
    }

    @Test
    void terminateStopsLongRunningProcess() throws Exception {
        try (ProcessSupervisor supervisor = new ProcessSupervisor()) {
            ProcessLaunchPlan plan = new ProcessLaunchPlan(
                    List.of("cmd.exe", "/c", "ping -n 30 127.0.0.1 >nul"),
                    tempDir,
                    Map.of()
            );

            ProcessSession session = supervisor.start(plan);
            assertEquals(ProcessStatus.RUNNING, session.getStatus());

            session.terminate();
            Integer exitCode = session.onExit().get(15, TimeUnit.SECONDS);

            assertTrue(exitCode != null);
            assertTrue(
                    session.getStatus() == ProcessStatus.EXITED || session.getStatus() == ProcessStatus.FAILED
            );
        }
    }

    private ProcessLaunchPlan windowsEchoPlan(String message) {
        List<String> command = new ArrayList<>();
        command.add("cmd.exe");
        command.add("/c");
        command.add("echo " + message);
        return new ProcessLaunchPlan(command, tempDir, Map.of());
    }
}
