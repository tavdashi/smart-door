package com.smartdoor.alerts;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable alert data model.
 */
public final class Alert {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AlertLevel    level;
    private final String        message;
    private final LocalDateTime timestamp;
    private final long          secondsOpen;

    public Alert(AlertLevel level, String message, long secondsOpen) {
        this.level       = level;
        this.message     = message;
        this.secondsOpen = secondsOpen;
        this.timestamp   = LocalDateTime.now();
    }

    public String toLogLine() {
        return String.format("[ALERT]  %s | %s | %s | Door open %ds",
            level.getIcon(), timestamp.format(FMT), message, secondsOpen);
    }

    public AlertLevel    getLevel()       { return level; }
    public String        getMessage()     { return message; }
    public LocalDateTime getTimestamp()   { return timestamp; }
    public long          getSecondsOpen() { return secondsOpen; }

    @Override
    public String toString() { return toLogLine(); }
}
