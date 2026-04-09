# Smart Door Monitoring System

A real-time IoT door monitoring system built with **Arduino Uno**, **Java**, and a **live web dashboard**. Built as an Object-Oriented Programming project demonstrating all five OOP pillars through actual hardware integration.

---

## What It Does

Press a button → Arduino detects it → Java processes the event → LEDs and buzzer fire → live browser dashboard updates instantly.

A cardboard door frame with a push button simulates a real magnetic door sensor. The entire system — circuit, Java app, and dashboard — reacts in real time.

---

## Hardware

| Component | Pin | Role |
|---|---|---|
| Push Button | Pin 2 | Door sensor (press = open, release = closed) |
| Red LED | Pin 13 | Door status indicator |
| Yellow LED | Pin 7 | Alert indicator |
| Passive Buzzer | Pin 8 | Audio feedback |
| 220Ω Resistor | — | Current limiter for red LED |

> **Note:** A push button was used as a creative substitute for a magnetic door sensor. The architecture is designed so swapping to a real sensor requires zero code changes — just a hardware swap.

---

## Project Structure

```
SmartDoorSystem/
├── src/com/smartdoor/
│   ├── main/           DoorMonitorSystem.java   ← entry point
│   ├── core/           IoTDevice, DoorSensor, SensorBridge, DualInputBridge
│   ├── lock/           DoorLock, LockState
│   ├── alerts/         AlertManager, Alert, AlertLevel
│   ├── storage/        EventLog
│   ├── display/        ConsoleDisplay
│   ├── model/          SensorEvent, EventType
│   └── server/         DashboardServer          ← WebSocket, no libraries
├── SmartDoorDashboard_Live.html
├── compile_and_run.bat
└── jserialcomm-2.11.4.jar
```

---

## How to Run

### Step 1 — Upload Arduino Sketch
Upload the `.ino` sketch to your Arduino Uno. Keep Serial Monitor **closed** before running Java.

### Step 2 — Enable Hardware Mode
In `DoorMonitorSystem.java`, set:
```java
private static final boolean HARDWARE_MODE = true;
```
In `DualInputBridge.java`, set your COM port:
```java
private static final String COM_PORT = "COM3"; // change to your port
```

### Step 3 — Run the Java App
```bash
# Windows
compile_and_run.bat
```

### Step 4 — Open the Dashboard
Open `SmartDoorDashboard_Live.html` in any browser. The status pill turns **green** when connected to the Java app live.

### Step 5 — Press the Button
Watch everything react — LEDs, buzzer, and dashboard all update instantly.

---

## ⌨️ Keyboard Controls (Hardware Mode)

| Key | Action |
|---|---|
| `O` + Enter | Simulate door OPEN |
| `C` + Enter | Simulate door CLOSE |
| `Ctrl+C` | Shutdown with summary |

---

## Alert Thresholds

| Duration Open | Alert Level |
|---|---|
| 10 seconds | MEDIUM — buzzer beep |
| 20 seconds | HIGH — buzzer beep |
| 35 seconds | CRITICAL — long buzzer |
| 12s after close | Auto-lock engages |

---

## Requirements

- Java 17+
- Arduino Uno + USB cable
- Windows (for `compile_and_run.bat`) — or adapt for Mac/Linux
- Any modern browser (Chrome, Firefox, Safari)

---

## Simulation Mode

To run without Arduino hardware, set `HARDWARE_MODE = false` in `DoorMonitorSystem.java`. The system uses `MagneticSensor` to generate random door events automatically. The dashboard still works in full.

---

*Built as an OOP course project — Java + Arduino + WebSocket from scratch.*
