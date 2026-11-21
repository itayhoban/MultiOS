package com.multios.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Explicit process launch instructions produced by a {@link RuntimePlugin}.
 * The command is an argument list suitable for {@link ProcessBuilder} (never a shell string).
 *
 * @param command               executable and arguments
 * @param workingDirectory      process working directory
 * @param environmentVariables  additional environment variables to apply for this launch
 */
public record ProcessLaunchPlan(
        List<String> command,
        Path workingDirectory,
        Map<String, String> environmentVariables
) {
    public ProcessLaunchPlan {
        Objects.requireNonNull(command, "command must not be null");
        if (command.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }
        command = List.copyOf(command);
        Objects.requireNonNull(workingDirectory, "workingDirectory must not be null");
        environmentVariables = environmentVariables == null
                ? Map.of()
                : Map.copyOf(environmentVariables);
    }
}
