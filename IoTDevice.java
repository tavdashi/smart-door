package com.smartdoor.core;

import com.smartdoor.model.EventType;
import com.smartdoor.model.SensorEvent;
import com.smartdoor.storage.EventLog;

/**
 * IoTDevice — Abstract Base Class
 *
 * Every IoT device in this system extends this class.
 * Demonstrates: Abstraction, Inheritance, Encapsulation, Template Method pattern.
 *
 * The base class owns:
 *   - Device identity (id, name, location)
 *   - Lifecycle management (connect, disconnect)
 *   - Shared event logging
 *
 * Subclasses must implement:
 *   - readSensor()      — hardware-specific reading logic
 *   - getStatusSummary() — device-specific status string
 */
public abstract class IoTDevice {

    // ── Encapsulated Fields ───────────────────────────────────────
    private final String     deviceId;
    private final String     deviceName;
    private final String     location;
    private       DeviceStatus status;
    protected     EventLog   eventLog;

    // ── Constructor ───────────────────────────────────────────────
    protected IoTDevice(String deviceId, String deviceName,
                        String location, EventLog eventLog) {
        this.deviceId   = deviceId;
        this.deviceName = deviceName;
        this.location   = location;
        this.eventLog   = eventLog;
        this.status     = DeviceStatus.OFFLINE;
    }

    // ── Lifecycle — Template Methods ──────────────────────────────

    /**
     * Connect the device — sets status ONLINE and logs the event.
     * All devices share this connect behaviour.
     */
    public void connect() {
        this.status = DeviceStatus.ONLINE;
        eventLog.addEvent(new SensorEvent(
            EventType.SENSOR_CONNECTED,
            deviceId,
            deviceName + " → CONNECTED @ " + location
        ));
        System.out.printf("[DEVICE] %-30s → CONNECTED%n", deviceName);
    }

    /**
     * Disconnect the device — sets status OFFLINE and logs the event.
     */
    public void disconnect() {
        this.status = DeviceStatus.OFFLINE;
        eventLog.addEvent(new SensorEvent(
            EventType.SENSOR_DISCONNECTED,
            deviceId,
            deviceName + " → DISCONNECTED"
        ));
        System.out.printf("[DEVICE] %-30s → DISCONNECTED%n", deviceName);
    }

    // ── Abstract Methods — Subclass Must Implement ────────────────

    /**
     * Read the sensor and process any state changes.
     * Implementation is specific to each device type.
     */
    public abstract void readSensor();

    /**
     * Return a one-line summary of the device's current status.
     */
    public abstract String getStatusSummary();

    // ── Getters ───────────────────────────────────────────────────
    public String      getDeviceId()   { return deviceId; }
    public String      getDeviceName() { return deviceName; }
    public String      getLocation()   { return location; }
    public DeviceStatus getStatus()    { return status; }
    public boolean     isOnline()      { return status == DeviceStatus.ONLINE; }

    protected void setStatus(DeviceStatus status) { this.status = status; }
}
