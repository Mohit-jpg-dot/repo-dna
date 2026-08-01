package com.repodna.util;

public final class AnsiColors {
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    public static final String GRAY = "\u001B[90m";

    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String ITALIC = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";

    public static final String RESET = "\u001B[0m";

    private AnsiColors() {}

    public static String strip(String text) {
        if (text == null) return null;
        return text.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    public static boolean isSupported() {
        if (System.console() == null) {
            return false;
        }
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().contains("win")) {
            return System.getenv("WT_SESSION") != null || System.getenv("ANSICON") != null;
        }
        return true;
    }
}
