package com.repodna.util;

import java.util.List;

public final class Console {
    private static boolean colorsEnabled = AnsiColors.isSupported();

    private Console() {}

    public static boolean isColorsEnabled() {
        return colorsEnabled;
    }

    public static void setColorsEnabled(boolean enabled) {
        colorsEnabled = enabled;
    }

    private static String colorize(String text, String... codes) {
        if (!colorsEnabled || text == null) return text;
        StringBuilder sb = new StringBuilder();
        for (String code : codes) {
            sb.append(code);
        }
        sb.append(text).append(AnsiColors.RESET);
        return sb.toString();
    }

    public static void header(String text) {
        println(colorize(text, AnsiColors.BOLD, AnsiColors.CYAN, AnsiColors.UNDERLINE));
    }

    public static void success(String text) {
        println(colorize("✓ ", AnsiColors.GREEN) + text);
    }

    public static void warning(String text) {
        println(colorize("⚠ ", AnsiColors.YELLOW) + text);
    }

    public static void error(String text) {
        System.err.println(colorize("✗ ", AnsiColors.RED) + text);
    }

    public static void info(String text) {
        println(colorize("ℹ ", AnsiColors.BLUE) + text);
    }

    public static void dim(String text) {
        println(colorize(text, AnsiColors.GRAY, AnsiColors.DIM));
    }

    public static void table(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) return;
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length && i < widths.length; i++) {
                if (row[i] != null && row[i].length() > widths[i]) {
                    widths[i] = row[i].length();
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
            sb.append(String.format("%-" + widths[i] + "s", headers[i]));
            if (i < headers.length - 1) sb.append(" | ");
        }
        println(colorize(sb.toString(), AnsiColors.BOLD));
        
        sb.setLength(0);
        for (int i = 0; i < headers.length; i++) {
            sb.append("-".repeat(widths[i]));
            if (i < headers.length - 1) sb.append("-+-");
        }
        println(colorize(sb.toString(), AnsiColors.GRAY));

        for (String[] row : rows) {
            sb.setLength(0);
            for (int i = 0; i < headers.length; i++) {
                String val = (i < row.length && row[i] != null) ? row[i] : "";
                sb.append(String.format("%-" + widths[i] + "s", val));
                if (i < headers.length - 1) sb.append(" | ");
            }
            println(sb.toString());
        }
    }

    public static void progressBar(String label, int value, int max, int width) {
        int progress = (int) Math.round((double) value / max * width);
        progress = Math.max(0, Math.min(width, progress));
        String bar = "█".repeat(progress) + "░".repeat(width - progress);
        println(String.format("%s  %s  %d/%d", label, colorize(bar, AnsiColors.CYAN), value, max));
    }

    public static void scoreBar(String label, int score) {
        String grade;
        String color;
        if (score >= 90) { grade = "A"; color = AnsiColors.GREEN; }
        else if (score >= 85) { grade = "B+"; color = AnsiColors.CYAN; }
        else if (score >= 80) { grade = "B"; color = AnsiColors.CYAN; }
        else if (score >= 75) { grade = "C+"; color = AnsiColors.YELLOW; }
        else if (score >= 70) { grade = "C"; color = AnsiColors.YELLOW; }
        else if (score >= 60) { grade = "D"; color = AnsiColors.MAGENTA; }
        else { grade = "F"; color = AnsiColors.RED; }

        int width = 20;
        int progress = (int) Math.round((double) score / 100 * width);
        progress = Math.max(0, Math.min(width, progress));
        String bar = "█".repeat(progress) + "░".repeat(width - progress);
        
        println(String.format("%-15s [%s] %d%% %s", 
            label, 
            colorize(bar, color), 
            score, 
            colorize(grade, color, AnsiColors.BOLD)));
    }

    public static void box(String title, List<String> lines) {
        int maxLen = title.length();
        for (String line : lines) {
            maxLen = Math.max(maxLen, AnsiColors.strip(line).length());
        }
        
        println("╔═ " + colorize(title, AnsiColors.BOLD) + " " + "═".repeat(maxLen - title.length() + 1) + "╗");
        for (String line : lines) {
            int padding = maxLen - AnsiColors.strip(line).length();
            println("║ " + line + " ".repeat(padding) + " ║");
        }
        println("╚" + "═".repeat(maxLen + 2) + "╝");
    }

    public static void divider() {
        println(colorize("-".repeat(50), AnsiColors.GRAY));
    }

    public static void blank() {
        System.out.println();
    }

    public static void print(String text) {
        System.out.print(text);
    }

    public static void println(String text) {
        System.out.println(text);
    }
}
