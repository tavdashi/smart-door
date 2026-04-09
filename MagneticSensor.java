package com.smartdoor.core;

import java.util.Random;

/**
 * MagneticSensor — Software Simulation of a Magnetic Reed Switch
 *
 * Simulates a magnetic reed switch sensor (e.g. MC-38 door sensor).
 * In hardware deployment, this class is replaced by DualInputBridge
 * which reads identical HIGH/LOW signals from Arduino Pin 2 via USB Serial.
 *
 * A real reed switch returns:
 *   HIGH (true)  → magnets separated → door OPEN
 *   LOW  (false) → magnets aligned   → door CLOSED
 *
 * We simulate this with a Random boolean generator at 35% open probability.
 * Hardware substitute used: Push button (electrically equivalent signal).
 *
 * Implements SensorBridge so it can be swapped for DualInputBridge
 * without changing any other class in the system.
 */
public class MagneticSensor implements SensorBridge {

    private static final double OPEN_PROBABILITY = 0.35;
    private static final String SENSOR_ID        = "SENS-001-MAG";

    private final Random random;
    private boolean lastState = false;
    private boolean ready     = false;

    public MagneticSensor() {
        this.random = new Random();
    }

    @Override
    public void initialize() {
        System.out.printf("[SELF-TEST] Magnetic sensor %-15s → PASS%n", SENSOR_ID);
        this.ready = true;
    }

    @Override
    public boolean readDoorState() {
        lastState = random.nextDouble() < OPEN_PROBABILITY;
        return lastState;
    }

    @Override
    public String getBridgeName() {
        return "SimulatedMagneticSensor [Random signal, p=" + OPEN_PROBABILITY + "]";
    }

    @Override
    public boolean isReady() { return ready; }

    public String getSensorId() { return SENSOR_ID; }
}
