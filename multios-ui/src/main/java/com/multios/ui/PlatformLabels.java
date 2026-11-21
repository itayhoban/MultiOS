package com.multios.ui;

import com.multios.api.PlatformType;

/**
 * Human-readable labels for {@link PlatformType} values shown in the UI.
 */
final class PlatformLabels {

    private PlatformLabels() {
    }

    static String displayName(PlatformType platformType) {
        return switch (platformType) {
            case WINDOWS_NATIVE -> "Windows Native";
            case JAVA_8 -> "Java 8";
            case JAVA_21 -> "Java 21";
            case WINDOWS_98 -> "Windows 98 / DOS";
            case PLAYSTATION_2 -> "PlayStation 2";
            case LINUX -> "Linux";
            case ANDROID -> "Android";
        };
    }
}
