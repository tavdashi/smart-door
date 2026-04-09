package com.smartdoor.display;

import com.smartdoor.alerts.AlertManager;
import com.smartdoor.core.DoorSensor;
import com.smartdoor.lock.DoorLock;
import com.smartdoor.storage.EventLog;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ConsoleDisplay — Live Terminal Dashboard
 *
 * Renders a clean real-time status panel to the console.
 * Called after every sensor poll to show current system state.
 */
public class ConsoleDisplay {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DoorSensor   doorSensor;
    private final DoorLock     doorLock;
    private final AlertManager alertManager;
    private final EventLog     eventLog;

    public ConsoleDisplay(DoorSensor doorSensor, DoorLock doorLock,
                          AlertManager alertManager, EventLog eventLog) {
        this.doorSensor   = doorSensor;
        this.doorLock     = doorLock;
        this.alertManager = alertManager;
        this.eventLog     = eventLog;
    }

    public void printStatusBar() {
        String doorState = doorSensor.isDoorOpen()
            ? "🔴 OPEN  " : "🟢 CLOSED";
        String lockState = doorLock.isLocked()
            ? "🔒 LOCKED  " : "🔓 UNLOCKED";
        String autoLock  = doorLock.isAutoLockScheduled()
            ? " [auto-lock pending]" : "";

        System.out.printf(
            "[%s] DOOR: %-10s | LOCK: %-12s%s | Events: %d | Alerts: %d%n",
            LocalDateTime.now().format(FMT),
            doorState, lockState, autoLock,
            eventLog.size(),
            alertManager.getAlertCount()
        );
    }

    public static void printBoot(String bridgeName) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║       SMART DOOR MONITORING SYSTEM — v2.0               ║");
        System.out.println("║       Java IoT Application                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("[BOOT] Active Bridge : " + bridgeName);
        System.out.println("[BOOT] Sensor        : Front Door Magnetic Sensor (SENS-001)");
        System.out.println("[BOOT] Location      : Front Door");
        System.out.println("[BOOT] Alert Thread  : Starting...");
        System.out.println("────────────────────────────────────────────────────────────");
    }

    public void printShutdownSummary() {
        int opens   = doorSensor.getTotalOpens();
        int closes  = doorSensor.getTotalCloses();
        int alerts  = alertManager.getAlertCount();
        int total   = eventLog.size();

        System.out.println("\n════════════════════ SESSION SUMMARY ═══════════════════════");
        System.out.printf("  Total Events Logged : %d%n", total);
        System.out.printf("  Door Opened         : %d times%n", opens);
        System.out.printf("  Door Closed         : %d times%n", closes);
        System.out.printf("  Alerts Fired        : %d%n", alerts);
        System.out.println("════════════════════════════════════════════════════════════");

        eventLog.printAll();
    }
}
