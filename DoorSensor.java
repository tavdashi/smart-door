package com.smartdoor.core;

import com.smartdoor.model.EventType;
import com.smartdoor.model.SensorEvent;
import com.smartdoor.storage.EventLog;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * DoorSensor — Concrete IoT Device Implementation
 *
 * Extends IoTDevice and models a physical door sensor.
 * Uses a SensorBridge to read door state — works with both
 * simulation (MagneticSensor) and real hardware (DualInputBridge).
 *
 * Key responsibilities:
 *   - Read sensor via SensorBridge
 *   - Detect state CHANGES (not every poll)
 *   - Create SensorEvents and store in EventLog
 *   - Track open duration for AlertManager
 *   - Notify DoorLock and AlertManager via callbacks
 *
 * Demonstrates: Inheritance, Encapsulation, Abstraction, Dependency Injection
 */
public class DoorSensor extends IoTDevice {

    // ── Encapsulated State ────────────────────────────────────────
    private boolean       isDoorOpen    = false;
    private LocalDateTime doorOpenedAt  = null;
    private int           totalOpens    = 0;
    private int           totalCloses   = 0;
    private String        lastInputSrc  = "SYSTEM";

    // ── Dependencies ──────────────────────────────────────────────
    private final SensorBridge bridge;

    // ── Callbacks ─────────────────────────────────────────────────
    private Runnable onDoorOpenCallback;
    private Runnable onDoorCloseCallback;

    // ── Constructors ──────────────────────────────────────────────

    /** Default constructor — uses MagneticSensor (simulation mode) */
    public DoorSensor(String deviceId, String location, EventLog eventLog) {
        super(deviceId, "Front Door Magnetic Sensor", location, eventLog);
        MagneticSensor sim = new MagneticSensor();
        sim.initialize();
        this.bridge = sim;
    }

    /** Hardware constructor — accepts any SensorBridge (simulation OR hardware) */
    public DoorSensor(String deviceId, String location,
                      EventLog eventLog, SensorBridge bridge) {
        super(deviceId, "Front Door Magnetic Sensor", location, eventLog);
        this.bridge = bridge;
        this.bridge.initialize();
    }

    // ── Callback Registration ─────────────────────────────────────
    public void setOnDoorOpen(Runnable callback)  { this.onDoorOpenCallback  = callback; }
    public void setOnDoorClose(Runnable callback) { this.onDoorCloseCallback = callback; }

    // ── Core Method: readSensor() — implements abstract from IoTDevice ──
    @Override
    public void readSensor() {
        if (!isOnline()) return;

        boolean signal = bridge.readDoorState();

        // Detect input source if using DualInputBridge
        if (bridge instanceof DualInputBridge) {
            lastInputSrc = ((DualInputBridge) bridge).getLastInputSource();
        } else {
            lastInputSrc = "SIMULATION";
        }

        // ── State Change Detection ────────────────────────────────
        // Only fire event when state CHANGES — not on every poll
        if (signal && !isDoorOpen) {
            onDoorOpened();
        } else if (!signal && isDoorOpen) {
            onDoorClosed();
        }
    }

    // ── Door Open Handler ─────────────────────────────────────────
    private void onDoorOpened() {
        isDoorOpen   = true;
        doorOpenedAt = LocalDateTime.now();
        totalOpens++;

        SensorEvent event = new SensorEvent(
            EventType.DOOR_OPENED,
            getDeviceId(),
            "Door Opened — CLOSED→OPEN",
            lastInputSrc
        );
        eventLog.addEvent(event);

        System.out.println(event.toLogLine());

        // Notify lock and alert systems
        if (onDoorOpenCallback != null) onDoorOpenCallback.run();
    }

    // ── Door Close Handler ────────────────────────────────────────
    private void onDoorClosed() {
        long secondsOpen = getSecondsOpen();
        isDoorOpen   = false;
        doorOpenedAt = null;
        totalCloses++;

        SensorEvent event = new SensorEvent(
            EventType.DOOR_CLOSED,
            getDeviceId(),
            String.format("Door Closed — OPEN→CLOSED (was open %ds)", secondsOpen),
            lastInputSrc
        );
        eventLog.addEvent(event);

        System.out.println(event.toLogLine());

        // Notify lock and alert systems
        if (onDoorCloseCallback != null) onDoorCloseCallback.run();
    }

    // ── Status ────────────────────────────────────────────────────
    @Override
    public String getStatusSummary() {
        return String.format(
            "DoorSensor[%s] | Location: %-12s | State: %-6s | Opens: %d | Closes: %d | Bridge: %s",
            getDeviceId(), getLocation(),
            isDoorOpen ? "OPEN" : "CLOSED",
            totalOpens, totalCloses,
            bridge.getBridgeName()
        );
    }

    // ── Getters ───────────────────────────────────────────────────
    public boolean       isDoorOpen()    { return isDoorOpen; }
    public int           getTotalOpens() { return totalOpens; }
    public int           getTotalCloses(){ return totalCloses; }
    public SensorBridge  getBridge()     { return bridge; }

    /**
     * Returns how many seconds the door has been continuously open.
     * AlertManager calls this to check if thresholds are breached.
     */
    public long getSecondsOpen() {
        if (!isDoorOpen || doorOpenedAt == null) return 0;
        return ChronoUnit.SECONDS.between(doorOpenedAt, LocalDateTime.now());
    }
}
