package com.multios.ui;

/**
 * Dedicated entry point for launching MultiOs on the classpath.
 * Avoids requiring {@code MultiOsApp} itself to host {@code main} under JavaFX module-path quirks on Windows.
 */
public final class MultiOsLauncher {

    private MultiOsLauncher() {
    }

    public static void main(String[] args) {
        MultiOsApp.launch(MultiOsApp.class, args);
    }
}
