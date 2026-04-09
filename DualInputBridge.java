package com.smartdoor.core;

import com.fazecast.jSerialComm.SerialPort;
import java.io.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DualInputBridge — Primary Hardware + Keyboard Backup
 *
 * PRIMARY:  Arduino push button via USB Serial (COM5)
 * BACKUP:   Keyboard input — O = open, C = close
 */
public class DualInputBridge implements SensorBridge {

    private static final String COM_PORT  = "COM5";
    private static final int    BAUD_RATE = 9600;

    private volatile boolean currentDoorState = false;
    private volatile String  lastInputSource  = "SYSTEM";
    private volatile boolean ready            = false;

    private SerialPort serialPort;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Runnable onStateChange;

    public void setOnStateChange(Runnable callback) {
        this.onStateChange = callback;
    }

    @Override
    public void initialize() {
        running.set(true);

        boolean arduinoConnected = initArduinoSerial();

        if (arduinoConnected) {
            System.out.println("[BRIDGE] Arduino connected on " + COM_PORT);
            System.out.println("[BRIDGE] PRIMARY: Push button | BACKUP: Keyboard (O/C)");
        } else {
            System.out.println("[BRIDGE] Arduino not found — keyboard-only mode");
            System.out.println("[BRIDGE] Type O = Door Open | C = Door Close");
        }

        startKeyboardListener();
        ready = true;
    }

    private boolean initArduinoSerial() {
        try {
            serialPort = SerialPort.getCommPort(COM_PORT);
            serialPort.setBaudRate(BAUD_RATE);
            serialPort.setComPortTimeouts(
            SerialPort.TIMEOUT_SCANNER, 0, 0
            );

            if (!serialPort.openPort()) {
                System.out.println("[BRIDGE] Could not open " + COM_PORT);
                return false;
            }

            Thread.sleep(2000); // wait for Arduino to boot
            sendArduinoCommand("SELF:TEST");

            startSerialListener();
            return true;

        } catch (Exception e) {
            System.out.println("[BRIDGE] Serial error: " + e.getMessage());
            return false;
        }
    }

    private void startSerialListener() {
        Thread serialThread = new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(serialPort.getInputStream())
                );
                while (running.get()) {
                    String line = reader.readLine();
                    if (line == null) continue;
                    line = line.trim();

                    if (line.equals("DOOR:OPEN")) {
                        currentDoorState = true;
                        lastInputSource  = "HARDWARE";
                        System.out.println("[HW] Button pressed → DOOR OPEN");
                        if (onStateChange != null) onStateChange.run();

                    } else if (line.equals("DOOR:CLOSED")) {
                        currentDoorState = false;
                        lastInputSource  = "HARDWARE";
                        System.out.println("[HW] Button released → DOOR CLOSED");
                        if (onStateChange != null) onStateChange.run();
                    }
                }
            } catch (IOException e) {
                if (running.get())
                    System.out.println("[BRIDGE] Serial read error: " + e.getMessage());
            }
        }, "SerialListenerThread");
        serialThread.setDaemon(true);
        serialThread.start();
    }

    private void startKeyboardListener() {
        Thread keyboardThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("[KB] Keyboard ready — O = open, C = close");

            while (running.get()) {
                try {
                    if (scanner.hasNextLine()) {
                        String input = scanner.nextLine().trim().toUpperCase();

                        if (input.equals("O") || input.equals("OPEN")) {
                            currentDoorState = true;
                            lastInputSource  = "KEYBOARD";
                            System.out.println("[KB] Keyboard → DOOR OPEN");
                            if (onStateChange != null) onStateChange.run();

                        } else if (input.equals("C") || input.equals("CLOSE")) {
                            currentDoorState = false;
                            lastInputSource  = "KEYBOARD";
                            System.out.println("[KB] Keyboard → DOOR CLOSED");
                            if (onStateChange != null) onStateChange.run();

                        } else if (!input.isEmpty()) {
                            System.out.println("[KB] Type O (open) or C (close)");
                        }
                    }
                } catch (Exception e) {
                    if (running.get())
                        System.out.println("[KB] Keyboard error: " + e.getMessage());
                }
            }
        }, "KeyboardListenerThread");
        keyboardThread.setDaemon(true);
        keyboardThread.start();
    }

    public void sendArduinoCommand(String command) {
        if (serialPort != null && serialPort.isOpen()) {
            try {
                byte[] bytes = (command + "\n").getBytes();
                serialPort.getOutputStream().write(bytes);
                serialPort.getOutputStream().flush();
                System.out.println("[CMD→Arduino] " + command);
            } catch (IOException e) {
                System.out.println("[BRIDGE] Send error: " + e.getMessage());
            }
        } else {
            System.out.println("[CMD→Arduino] " + command + " (simulation)");
        }
    }

    @Override
    public boolean readDoorState() { return currentDoorState; }

    @Override
    public String getBridgeName() {
        return "DualInputBridge [Primary: Arduino Button COM5 | Backup: Keyboard O/C]";
    }

    @Override
    public boolean isReady() { return ready; }

    @Override
    public void shutdown() {
        running.set(false);
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }

    public String getLastInputSource() { return lastInputSource; }
}
