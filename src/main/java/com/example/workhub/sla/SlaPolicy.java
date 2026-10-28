package com.example.workhub.sla;

public final class SlaPolicy {
    private SlaPolicy() {}
    /** Example service targets, not contractual SLAs. */
    public static int minutesFor(String priority) {
        if (priority == null) return 1440;
        return switch (priority) {
            case "URGENT" -> 60;
            case "HIGH" -> 240;
            case "LOW" -> 4320;
            case "MEDIUM" -> 1440;
            default -> throw new IllegalArgumentException("Invalid priority: " + priority);
        };
    }
}
