package com.repodna.util;

public class ProgressReporter {
    private final int totalSteps;
    private int currentStep;
    private long startTime;
    private long stepStartTime;
    private String currentDescription;

    public ProgressReporter(int totalSteps) {
        this.totalSteps = totalSteps;
        this.startTime = System.nanoTime();
        this.currentStep = 0;
    }

    public void startStep(String description) {
        currentStep++;
        currentDescription = description;
        stepStartTime = System.nanoTime();
        Console.print(String.format("[%d/%d] %s... ", currentStep, totalSteps, description));
    }

    public void completeStep() {
        long durationMs = (System.nanoTime() - stepStartTime) / 1_000_000;
        Console.print(AnsiColors.GREEN + "✓" + AnsiColors.RESET + String.format(" (%dms)%n", durationMs));
    }

    public void failStep(String reason) {
        Console.print(AnsiColors.RED + "✗" + AnsiColors.RESET + "\n");
        Console.error("Failed: " + reason);
    }

    public void finish() {
        long totalDurationMs = (System.nanoTime() - startTime) / 1_000_000;
        Console.blank();
        Console.success(String.format("Completed %d steps in %dms", currentStep, totalDurationMs));
    }
}
