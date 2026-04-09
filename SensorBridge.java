package com.smartdoor.core;

/**
 * SensorBridge — Hardware Abstraction Interface
 *
 * This interface is the seam between simulation and real hardware.
 * Two implementations exist:
 *   1. MagneticSensor   — software simulation using Random
 *   2. DualInputBridge  — real hardware (Arduino button) + keyboard backup
 *
 * DoorSensor only ever talks to a SensorBridge.
 * It has no idea whether the signal is from a random generator,
 * a push button, or a real magnetic reed switch. That is the abstraction.
 */
public interface SensorBridge {

    /**
     * Read the current door state.
     * @return true if door is OPEN, false if door is CLOSED
     */
    boolean readDoorState();

    /**
     * @return Human-readable name of this bridge implementation
     */
    String getBridgeName();

    /**
     * @return true if this bridge is ready to read
     */
    boolean isReady();

    /**
     * Called once at startup. Override for initialization logic.
     */
    default void initialize() {}

    /**
     * Called on system shutdown. Override for cleanup logic.
     */
    default void shutdown() {}
}
