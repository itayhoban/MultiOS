package com.multios.core;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handle for a single supervised OS process, exposing live status and lifecycle controls.
 */
public final class ProcessSession {

    private final Process process;
    private final AtomicReference<ProcessStatus> status;
    private final CompletableFuture<Integer> completion;
    private final AtomicReference<Throwable> failureCause;

    ProcessSession(Process process) {
        this.process = Objects.requireNonNull(process, "process must not be null");
        this.status = new AtomicReference<>(ProcessStatus.RUNNING);
        this.completion = new CompletableFuture<>();
        this.failureCause = new AtomicReference<>();
    }

    public ProcessStatus getStatus() {
        return status.get();
    }

    public Optional<Integer> getExitCode() {
        if (status.get() == ProcessStatus.RUNNING) {
            return Optional.empty();
        }
        try {
            return Optional.of(process.exitValue());
        } catch (IllegalThreadStateException e) {
            return Optional.empty();
        }
    }

    public Optional<Throwable> getFailureCause() {
        return Optional.ofNullable(failureCause.get());
    }

    public CompletableFuture<Integer> onExit() {
        return completion;
    }

    public long pid() {
        return process.pid();
    }

    /**
     * Terminates the process tree: destroys all descendants first, then the root process.
     */
    public void terminate() {
        try {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        } catch (Exception e) {
            markFailed(e);
        }
    }

    void markExited(int exitCode) {
        status.compareAndSet(ProcessStatus.RUNNING, ProcessStatus.EXITED);
        completion.complete(exitCode);
    }

    void markFailed(Throwable cause) {
        failureCause.compareAndSet(null, cause);
        status.set(ProcessStatus.FAILED);
        completion.completeExceptionally(cause);
    }

    Process process() {
        return process;
    }
}
