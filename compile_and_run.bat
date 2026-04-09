@echo off
echo === Smart Door Monitoring System - Build Script ===
echo.

echo [1/3] Compiling all Java files...
if not exist out mkdir out

javac -cp jserialcomm-2.11.4.jar -d out ^
  src\com\smartdoor\model\EventType.java ^
  src\com\smartdoor\model\SensorEvent.java ^
  src\com\smartdoor\core\DeviceStatus.java ^
  src\com\smartdoor\core\SensorBridge.java ^
  src\com\smartdoor\core\IoTDevice.java ^
  src\com\smartdoor\core\MagneticSensor.java ^
  src\com\smartdoor\core\DualInputBridge.java ^
  src\com\smartdoor\storage\EventLog.java ^
  src\com\smartdoor\core\DoorSensor.java ^
  src\com\smartdoor\lock\LockState.java ^
  src\com\smartdoor\lock\DoorLock.java ^
  src\com\smartdoor\alerts\AlertLevel.java ^
  src\com\smartdoor\alerts\Alert.java ^
  src\com\smartdoor\alerts\AlertManager.java ^
  src\com\smartdoor\display\ConsoleDisplay.java ^
  src\com\smartdoor\server\DashboardServer.java ^
  src\com\smartdoor\main\DoorMonitorSystem.java

if %errorlevel% neq 0 (
    echo.
    echo FAILED - Compilation errors above.
    pause
    exit /b 1
)

echo.
echo [2/3] Compilation successful!
echo.
echo [3/3] Starting Smart Door Monitor...
echo       Press Ctrl+C to stop
echo       Close Arduino Serial Monitor first!
echo.
java -cp out;jserialcomm-2.11.4.jar com.smartdoor.main.DoorMonitorSystem
pause
