package com.multios.core;

import com.multios.api.ProcessLaunchPlan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Starts processes from a {@link ProcessLaunchPlan}, streams stdout/stderr on virtual threads,
 * and tracks lifecycle via {@link ProcessSession}.
 */
public final class ProcessSupervisor implements AutoCloseable {

    private final ExecutorService virtualExecutor;

    public ProcessSupervisor() {
        this.virtualExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("multios-proc-", 0).factory());
    }

    public ProcessSession start(ProcessLaunchPlan plan) {
        return start(plan, (stream, line) -> {
        });
    }

    public ProcessSession start(ProcessLaunchPlan plan, ProcessOutputListener listener) {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(listener, "listener must not be null");

        ProcessBuilder builder = new ProcessBuilder(plan.command());
        builder.directory(plan.workingDirectory().toFile());
        Map<String, String> environment = builder.environment();
        environment.putAll(plan.environmentVariables());

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start process: " + plan.command(), e);
        }

        ProcessSession session = new ProcessSession(process);
        virtualExecutor.submit(() -> drainStream(process.getInputStream(), ProcessOutputStream.STDOUT, listener, session));
        virtualExecutor.submit(() -> drainStream(process.getErrorStream(), ProcessOutputStream.STDERR, listener, session));
        virtualExecutor.submit(() -> awaitExit(process, session));
        return session;
    }

    private void drainStream(
            InputStream inputStream,
            ProcessOutputStream streamType,
            ProcessOutputListener listener,
            ProcessSession session
    ) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                listener.onLine(streamType, line);
            }
        } catch (IOException e) {
            if (session.getStatus() == ProcessStatus.RUNNING) {
                session.markFailed(e);
            }
        }
    }

    private void awaitExit(Process process, ProcessSession session) {
        try {
            int exitCode = process.waitFor();
            session.markExited(exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            session.markFailed(e);
            session.terminate();
        }
    }

    @Override
    public void close() {
        virtualExecutor.shutdownNow();
    }
}
