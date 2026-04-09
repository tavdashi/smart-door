package com.smartdoor.model;

/**
 * Defines every possible event type in the Smart Door Monitoring System.
 * Using enums instead of raw strings ensures type-safety — invalid event
 * types are caught at compile time, not runtime.
 */
public enum EventType {

    // ── Door Events ──────────────────────────────────────────────
    DOOR_OPENED         ("[OPEN]  ", "Door Opened"),
    DOOR_CLOSED         ("[CLOSE] ", "Door Closed"),

    // ── Lock Events ──────────────────────────────────────────────
    DOOR_LOCKED         ("[LOCK]  ", "Door Locked"),
    DOOR_UNLOCKED       ("[UNLOCK]", "Door Unlocked"),
    AUTO_LOCK_SCHEDULED ("[AUTO]  ", "Auto-Lock Scheduled"),
    AUTO_LOCK_CANCELLED ("[AUTO]  ", "Auto-Lock Cancelled"),

    // ── Alert Events ─────────────────────────────────────────────
    ALERT_DOOR_OPEN_TOO_LONG    ("[ALERT] ", "Door Open Too Long"),
    ALERT_UNLOCK_WHILE_OPEN     ("[ALERT] ", "Unlocked While Door Open"),
    ALERT_CLEARED               ("[CLEAR] ", "Alert Cleared"),

    // ── System Events ────────────────────────────────────────────
    SYSTEM_START        ("[SYS]   ", "System Started"),
    SYSTEM_STOP         ("[SYS]   ", "System Stopped"),
    SENSOR_CONNECTED    ("[SYS]   ", "Sensor Connected"),
    SENSOR_DISCONNECTED ("[SYS]   ", "Sensor Disconnected"),
    SELF_TEST_PASS      ("[TEST]  ", "Self-Test Passed"),
    SELF_TEST_FAIL      ("[TEST]  ", "Self-Test Failed"),

    // ── Input Source Events ──────────────────────────────────────
    INPUT_HARDWARE      ("[HW]    ", "Hardware Trigger (Button)"),
    INPUT_KEYBOARD      ("[KB]    ", "Keyboard Trigger");

    // ── Fields ───────────────────────────────────────────────────
    private final String tag;
    private final String description;

    EventType(String tag, String description) {
        this.tag         = tag;
        this.description = description;
    }

    public String getTag()         { return tag; }
    public String getDescription() { return description; }
}
