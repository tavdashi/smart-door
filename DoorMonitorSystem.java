package com.smartdoor.main;

import com.smartdoor.alerts.AlertManager;
import com.smartdoor.core.DoorSensor;
import com.smartdoor.core.DualInputBridge;
import com.smartdoor.core.MagneticSensor;
import com.smartdoor.core.SensorBridge;
import com.smartdoor.display.ConsoleDisplay;
import com.smartdoor.lock.DoorLock;
import com.smartdoor.model.EventType;
import com.smartdoor.model.SensorEvent;
import com.smartdoor.server.DashboardServer;
import com.smartdoor.storage.EventLog;

import java.util.Timer;
import java.util.TimerTask;

/**
 * DoorMonitorSystem — Entry Point & System Orchestrator
 *
 * Wires all components together and starts the three-thread architecture:
 *
 *   Thread 1 — MAIN THREAD:    Startup, shutdown, display
 *   Thread 2 — SENSOR TIMER:   Polls sensor every 4 seconds (simulation mode)
 *                               In hardware mode, sensor thread is event-driven
 *   Thread 3 — ALERT THREAD:   Checks open duration every 2 seconds
 *
 * ── SWITCHING BETWEEN SIMULATION AND HARDWARE ────────────────────
 *
 *   SIMULATION (current):
 *     SensorBridge bridge = new MagneticSensor();
 *
 *   HARDWARE (Arduino connected):
 *     SensorBridge bridge = new DualInputBridge();
 *
 *   That is the ONLY line that changes. Everything else is identical.
 *   This is the SensorBridge abstraction paying off.
 * ──────────────────────────────────────────────────────────────────
 */
public class DoorMonitorSystem {

    // ── Configuration ─────────────────────────────────────────────
    private static final int  POLL_INTERVAL_MS = 4000; // 4 seconds
    private static final int  RUN_DURATION_MS  = 0;    // 0 = run until user quits

    // ── MODE SELECT ───────────────────────────────────────────────
    // To switch to hardware mode: change 'false' to 'true'
    private static final boolean HARDWARE_MODE = true;

    public static void main(String[] args) throws InterruptedException {

        // ── 1. Create shared EventLog first ───────────────────────
        EventLog eventLog = new EventLog();

        // ── 1b. Start Dashboard WebSocket server ──────────────────
        DashboardServer dashboard = new DashboardServer(8887);
        dashboard.start();

        // ── 2. Select SensorBridge based on mode ──────────────────
        SensorBridge bridge;
        if (HARDWARE_MODE) {
            // ── HARDWARE MODE: Arduino button + keyboard backup ───
            DualInputBridge dualBridge = new DualInputBridge();
            bridge = dualBridge;
            System.out.println("[BOOT] Mode: HARDWARE (Arduino Button + Keyboard O/C)");
        } else {
            // ── SIMULATION MODE: Random signal generator ──────────
            bridge = new MagneticSensor();
            System.out.println("[BOOT] Mode: SIMULATION (MagneticSensor → Random signal)");
            System.out.println("[BOOT] To switch to hardware: set HARDWARE_MODE = true");
        }

        ConsoleDisplay.printBoot(bridge.getBridgeName());

        // ── 3. Create DoorSensor with selected bridge ──────────────
        DoorSensor doorSensor = new DoorSensor("SENS-001", "Front Door", eventLog, bridge);
        doorSensor.connect();

        // ── 4. Create DoorLock linked to DoorSensor ────────────────
        DoorLock doorLock = new DoorLock(doorSensor, eventLog);

        // ── 5. Create AlertManager ────────────────────────────────
        AlertManager alertManager = new AlertManager(doorSensor, eventLog);

        // ── 6. Create Display ─────────────────────────────────────
        ConsoleDisplay display = new ConsoleDisplay(
            doorSensor, doorLock, alertManager, eventLog
        );

        // ── 7. Wire DoorSensor callbacks to DoorLock ──────────────
        doorSensor.setOnDoorOpen(() -> {
            doorLock.cancelAutoLock();
            dashboard.sendDoorOpen();
            // Send LED on to Arduino
            if (bridge instanceof DualInputBridge) {
                ((DualInputBridge) bridge).sendArduinoCommand("LED:ON");
                ((DualInputBridge) bridge).sendArduinoCommand("BUZZ:SHORT");
            }
        });

        doorSensor.setOnDoorClose(() -> {
            doorLock.scheduleAutoLock();
            dashboard.sendDoorClose();
            // Send LED off to Arduino
            if (bridge instanceof DualInputBridge) {
                ((DualInputBridge) bridge).sendArduinoCommand("LED:OFF");
            }
            // Notify dashboard when auto-lock fires (12s after close)
            new Thread(() -> {
                try {
                    Thread.sleep(12000);
                    if (!doorSensor.isDoorOpen()) dashboard.sendLock();
                } catch (InterruptedException ignored) {}
            }).start();
        });

        // ── 8. Log system start ───────────────────────────────────
        eventLog.addEvent(new SensorEvent(
            EventType.SYSTEM_START, "SYSTEM", "Smart Door Monitor started"
        ));

        // ── 9. Start Alert Thread (Thread 3) ──────────────────────
        Thread alertThread = new Thread(alertManager, "AlertManagerThread");
        alertThread.setDaemon(true);
        alertThread.start();

        // ── 10. Start Sensor Polling (Thread 2) ───────────────────
        // In simulation mode: timer fires every 4s to check random sensor
        // In hardware mode: DualInputBridge keyboard thread is already running
        Timer sensorTimer = null;

        if (!HARDWARE_MODE) {
            sensorTimer = new Timer("SensorPollTimer", true);
            sensorTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    doorSensor.readSensor();
                    display.printStatusBar();
                }
            }, 1000, POLL_INTERVAL_MS);
            System.out.println("[BOOT] Sensor polling timer started (every 4s)");
        } else {
            // Hardware mode: DualInputBridge fires callbacks on button press
            // We just need a display refresh timer
            DualInputBridge dualBridge = (DualInputBridge) bridge;
            dualBridge.setOnStateChange(() -> {
                doorSensor.readSensor();
                display.printStatusBar();
            });
            System.out.println("[BOOT] Hardware event-driven mode active");
            System.out.println("[BOOT] Press button on breadboard OR type O/C + Enter");
        }

        System.out.println("[BOOT] System running. Press Ctrl+C to stop.\n");

        // ── 11. Run until interrupted ─────────────────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SHUTDOWN] Stopping system...");
            alertManager.stop();
            doorSensor.disconnect();
            dashboard.stop();
            if (bridge instanceof DualInputBridge) {
                ((DualInputBridge) bridge).shutdown();
            }
            eventLog.addEvent(new SensorEvent(
                EventType.SYSTEM_STOP, "SYSTEM", "Smart Door Monitor stopped"
            ));
            display.printShutdownSummary();
        }));

        // Keep main thread alive
        Thread.currentThread().join();
    }
}
