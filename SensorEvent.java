package com.smartdoor.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable data model representing a single event in the system.
 * All fields are final — once an event is recorded, it cannot be changed.
 * This mirrors real IoT behaviour: a physical event that happened is a fact.
 */
public final class SensorEvent {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Immutable Fields ─────────────────────────────────────────
    private final EventType     type;
    private final LocalDateTime timestamp;
    private final String        sensorId;
    private final String        message;
    private final String        inputSource; // "HARDWARE" or "KEYBOARD"

    // ── Constructor ──────────────────────────────────────────────
    public SensorEvent(EventType type, String sensorId,
                       String message, String inputSource) {
        this.type        = type;
        this.sensorId    = sensorId;
        this.message     = message;
        this.inputSource = inputSource;
        this.timestamp   = LocalDateTime.now(); // captured at moment of creation
    }

    public SensorEvent(EventType type, String sensorId, String message) {
        this(type, sensorId, message, "SYSTEM");
    }

    // ── Formatting ───────────────────────────────────────────────
    public String toLogLine() {
        return String.format("%s %s | %-28s | Sensor: %-12s | Src: %s",
            type.getTag(),
            timestamp.format(FORMATTER),
            message,
            sensorId,
            inputSource
        );
    }

    // ── Getters ──────────────────────────────────────────────────
    public EventType     getType()        { return type; }
    public LocalDateTime getTimestamp()   { return timestamp; }
    public String        getSensorId()    { return sensorId; }
    public String        getMessage()     { return message; }
    public String        getInputSource() { return inputSource; }

    @Override
    public String toString() { return toLogLine(); }
}
