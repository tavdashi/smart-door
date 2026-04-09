package com.smartdoor.lock;

import com.smartdoor.core.DoorSensor;
import com.smartdoor.core.DualInputBridge;
import com.smartdoor.model.EventType;
import com.smartdoor.model.SensorEvent;
import com.smartdoor.storage.EventLog;

import java.util.Timer;
import java.util.TimerTask;

/**
 * DoorLock — Electronic Lock Controller
 *
 * Models an electronic deadbolt linked to the DoorSensor.
 * Features:
 *   - Manual lock/unlock with safety check
 *   - Auto-lock after 12 seconds when door closes
 *   - Sends hardware commands to Arduino (LED, buzzer) via DualInputBridge
 *
 * Demonstrates: Composition, Timer-based deferred actions, Safety validation
 */
public class DoorLock {

    private static final int AUTO_LOCK_DELAY_SECONDS = 12;

    // ── State ─────────────────────────────────────────────────────
    private LockState   lockState = LockState.LOCKED;
    private Timer       autoLockTimer;
    private boolean     autoLockScheduled = false;

    // ── Dependencies ──────────────────────────────────────────────
    private final DoorSensor  doorSensor;
    private final EventLog    eventLog;
    private       DualInputBridge bridge; // for sending hardware commands

    // ── Constructor ───────────────────────────────────────────────
    public DoorLock(DoorSensor doorSensor, EventLog eventLog) {
        this.doorSensor = doorSensor;
        this.eventLog   = eventLog;

        // Check if using DualInputBridge for hardware commands
        if (doorSensor.getBridge() instanceof DualInputBridge) {
            this.bridge = (DualInputBridge) doorSensor.getBridge();
        }
    }

    // ── Lock ──────────────────────────────────────────────────────
    public void lock() {
        lockState = LockState.LOCKED;
        cancelAutoLock();

        eventLog.addEvent(new SensorEvent(
            EventType.DOOR_LOCKED,
            doorSensor.getDeviceId(),
            "Door locked"
        ));
        System.out.println("[LOCK]   Door LOCKED 🔒");

        // Send buzzer command to Arduino
        sendCommand("BUZZ:LOCK");
        sendCommand("LED:OFF");
    }

    // ── Unlock ────────────────────────────────────────────────────
    public void unlock() {
        // Safety check: don't unlock silently if door is already open
        if (doorSensor.isDoorOpen()) {
            eventLog.addEvent(new SensorEvent(
                EventType.ALERT_UNLOCK_WHILE_OPEN,
                doorSensor.getDeviceId(),
                "WARNING: Unlock command received while door is already open!"
            ));
            System.out.println("[ALERT]  Unlock while door open — safety flag raised ⚠️");
            sendCommand("BUZZ:ALERT");
        }

        lockState = LockState.UNLOCKED;
        eventLog.addEvent(new SensorEvent(
            EventType.DOOR_UNLOCKED,
            doorSensor.getDeviceId(),
            "Door unlocked"
        ));
        System.out.println("[LOCK]   Door UNLOCKED 🔓");
    }

    // ── Auto-Lock Logic ───────────────────────────────────────────

    /** Called by DoorSensor when door closes — starts auto-lock countdown */
    public void scheduleAutoLock() {
        cancelAutoLock();
        autoLockScheduled = true;

        eventLog.addEvent(new SensorEvent(
            EventType.AUTO_LOCK_SCHEDULED,
            doorSensor.getDeviceId(),
            "Auto-lock in " + AUTO_LOCK_DELAY_SECONDS + " seconds"
        ));
        System.out.println("[AUTO]   Auto-lock scheduled in " + AUTO_LOCK_DELAY_SECONDS + "s");

        autoLockTimer = new Timer("AutoLockTimer", true);
        autoLockTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!doorSensor.isDoorOpen()) {
                    System.out.println("[AUTO]   Auto-lock fired → Door auto-locked ✅");
                    lock();
                    autoLockScheduled = false;
                }
            }
        }, AUTO_LOCK_DELAY_SECONDS * 1000L);
    }

    /** Called by DoorSensor when door opens — cancels pending auto-lock */
    public void cancelAutoLock() {
        if (autoLockTimer != null) {
            autoLockTimer.cancel();
            autoLockTimer = null;
        }
        if (autoLockScheduled) {
            autoLockScheduled = false;
            eventLog.addEvent(new SensorEvent(
                EventType.AUTO_LOCK_CANCELLED,
                doorSensor.getDeviceId(),
                "Auto-lock cancelled — door reopened"
            ));
            System.out.println("[AUTO]   Auto-lock cancelled — door reopened");
        }
    }

    // ── Hardware Command ──────────────────────────────────────────
    private void sendCommand(String cmd) {
        if (bridge != null) bridge.sendArduinoCommand(cmd);
    }

    // ── Getters ───────────────────────────────────────────────────
    public LockState getLockState()        { return lockState; }
    public boolean   isLocked()            { return lockState == LockState.LOCKED; }
    public boolean   isAutoLockScheduled() { return autoLockScheduled; }
}
