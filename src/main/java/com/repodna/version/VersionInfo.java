package com.repodna.version;

import java.time.Instant;

/**
 * Provides runtime version and environment metadata.
 */
public class VersionInfo {
    private static final String VERSION = "1.0.0";
    private static final String BUILD_DATE = "2026-08-01T12:00:00Z";
    private static final String GIT_COMMIT = "a0ad0e5";

    public static String getVersion() {
        return VERSION;
    }

    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    public static String getOsName() {
        return System.getProperty("os.name");
    }

    public static String getOsArch() {
        return System.getProperty("os.arch");
    }

    public static String getBuildDate() {
        return BUILD_DATE;
    }

    public static String getGitCommit() {
        return GIT_COMMIT;
    }
}
