package com.smartdoor.alerts;

import com.smartdoor.core.DoorSensor;
import com.smartdoor.core.DualInputBridge;
import com.smartdoor.model.EventType;
import com.smartdoor.model.SensorEvent;
import com.smartdoor.storage.EventLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AlertManager — Background Alert Monitor
 *
 * Runs on its own daemon thread. Every 2 seconds, checks how long
 * the door has been open and escalates alerts through three levels:
 *
 *   10 seconds → MEDIUM alert
 *   20 seconds → HIGH alert
 *   35 seconds → CRITICAL alert
 *
 * Each level fires exactly once per open event.
 * Resets completely when door closes.
 *
 * Demonstrates: Runnable, daemon threads, state machine pattern
 */
public class AlertManager implements Runnable {

    // ── Alert Thresholds (seconds) ────────────────────────────────
    private static final long MEDIUM_THRESHOLD   = 10;
    private static final long HIGH_THRESHOLD     = 20;
    private static final long CRITICAL_THRESHOLD = 35;

    // ── State ─────────────────────────────────────────────────────
    private AlertLevel       lastAlertLevel = null; // tracks highest level fired
    private boolean          running        = true;
    private final List<Alert> alertHistory  = new ArrayList<>();

    // ── Dependencies ──────────────────────────────────────────────
    private final DoorSensor doorSensor;
    private final EventLog   eventLog;
    private       DualInputBridge bridge;

    // ── Constructor ───────────────────────────────────────────────
    public AlertManager(DoorSensor doorSensor, EventLog eventLog) {
        this.doorSensor = doorSensor;
        this.eventLog   = eventLog;

        if (doorSensor.getBridge() instanceof DualInputBridge) {
            this.bridge = (DualInputBridge) doorSensor.getBridge();
        }
    }

    // ── Runnable — runs on its own thread ─────────────────────────
    @Override
    public void run() {
        System.out.println("[ALERT]  AlertManager thread started ✅");

        while (running) {
            try {
                Thread.sleep(2000); // check every 2 seconds

                if (!doorSensor.isOnline()) continue;

                if (doorSensor.isDoorOpen()) {
                    checkAlertThresholds();
                } else {
                    resetAlertState(); // door closed — reset escalation
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[ALERT]  AlertManager thread stopped");
    }

    // ── Threshold Checking ────────────────────────────────────────
    private void checkAlertThresholds() {
        long seconds = doorSensor.getSecondsOpen();

        if (seconds >= CRITICAL_THRESHOLD && lastAlertLevel != AlertLevel.CRITICAL) {
            raiseAlert(AlertLevel.CRITICAL,
                "Door open for " + seconds + "s — CRITICAL security risk!", seconds);
            sendCommand("BUZZ:CRITICAL");

        } else if (seconds >= HIGH_THRESHOLD && lastAlertLevel == AlertLevel.MEDIUM) {
            raiseAlert(AlertLevel.HIGH,
                "Door open for " + seconds + "s — HIGH alert", seconds);
            sendCommand("BUZZ:ALERT");

        } else if (seconds >= MEDIUM_THRESHOLD && lastAlertLevel == null) {
            raiseAlert(AlertLevel.MEDIUM,
                "Door open for " + seconds + "s — please close the door", seconds);
            sendCommand("BUZZ:ALERT");
        }
    }

    // ── Raise Alert ───────────────────────────────────────────────
    private void raiseAlert(AlertLevel level, String message, long secondsOpen) {
        lastAlertLevel = level;

        Alert alert = new Alert(level, message, secondsOpen);
        alertHistory.add(alert);

        eventLog.addEvent(new SensorEvent(
            EventType.ALERT_DOOR_OPEN_TOO_LONG,
            doorSensor.getDeviceId(),
            level.getIcon() + " " + level.getLabel() + " — " + message
        ));

        System.out.println(alert.toLogLine());
    }

    // ── Reset on Door Close ───────────────────────────────────────
    private void resetAlertState() {
        if (lastAlertLevel != null) {
            lastAlertLevel = null;
            eventLog.addEvent(new SensorEvent(
                EventType.ALERT_CLEARED,
                doorSensor.getDeviceId(),
                "Alert state reset — door closed"
            ));
        }
    }

    // ── Hardware Command ──────────────────────────────────────────
    private void sendCommand(String cmd) {
        if (bridge != null) bridge.sendArduinoCommand(cmd);
    }

    // ── Control ───────────────────────────────────────────────────
    public void stop() { running = false; }

    // ── Getters ───────────────────────────────────────────────────
    public List<Alert> getAlertHistory() {
        return Collections.unmodifiableList(alertHistory);
    }

    public int getAlertCount() { return alertHistory.size(); }
}
