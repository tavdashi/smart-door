/*
 * Smart Door Monitoring System — Arduino Sketch FIXED
 * Pins: LED1=13, LED2=12, Buzzer=8, Button=2
 */

const int LED1_PIN   = 13;
const int LED2_PIN   = 12;
const int BUZZER_PIN = 8;
const int BUTTON_PIN = 2;

bool lastButtonState = HIGH;
bool doorOpen        = false;

void setup() {
  pinMode(LED1_PIN,   OUTPUT);
  pinMode(LED2_PIN,   OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(BUTTON_PIN, INPUT_PULLUP);

  digitalWrite(LED1_PIN,  LOW);
  digitalWrite(LED2_PIN,  HIGH); // locked on boot
  digitalWrite(BUZZER_PIN, LOW);

  Serial.begin(9600);
  delay(1000);

  selfTest();
  Serial.println("SYSTEM:READY");
}

void loop() {
  // Read button
  bool currentState = digitalRead(BUTTON_PIN);

  // Detect change
  if (currentState != lastButtonState) {
    delay(80); // debounce
    currentState = digitalRead(BUTTON_PIN);

    if (currentState != lastButtonState) {
      lastButtonState = currentState;

      if (currentState == LOW) {
        doorOpen = true;
        digitalWrite(LED1_PIN, HIGH);
        digitalWrite(LED2_PIN, LOW);
        tone(BUZZER_PIN, 1000, 100);
        Serial.println("DOOR:OPEN");
        Serial.flush(); // make sure it sends immediately

      } else {
        doorOpen = false;
        digitalWrite(LED1_PIN, LOW);
        Serial.println("DOOR:CLOSED");
        Serial.flush();
      }
    }
  }

  // Read commands from Java
  if (Serial.available() > 0) {
    String cmd = Serial.readStringUntil('\n');
    cmd.trim();
    handleCommand(cmd);
  }

  delay(10); // faster loop = more responsive
}

void handleCommand(String cmd) {
  if (cmd == "LED:ON") {
    digitalWrite(LED1_PIN, HIGH);

  } else if (cmd == "LED:OFF") {
    digitalWrite(LED1_PIN, LOW);

  } else if (cmd == "LED2:ON") {
    digitalWrite(LED2_PIN, HIGH);

  } else if (cmd == "LED2:OFF") {
    digitalWrite(LED2_PIN, LOW);

  } else if (cmd == "BUZZ:SHORT") {
    tone(BUZZER_PIN, 1000, 100);
    delay(150);

  } else if (cmd == "BUZZ:LOCK") {
    tone(BUZZER_PIN, 1200, 150);
    delay(200);
    tone(BUZZER_PIN, 1200, 150);
    delay(200);
    digitalWrite(LED2_PIN, HIGH);

  } else if (cmd == "BUZZ:ALERT") {
    for (int i = 0; i < 5; i++) {
      tone(BUZZER_PIN, 2000, 80);
      delay(120);
    }

  } else if (cmd == "BUZZ:CRITICAL") {
    tone(BUZZER_PIN, 2500, 1000);
    delay(1100);

  } else if (cmd == "SELF:TEST") {
    selfTest();
    Serial.println("SELF:TEST:PASS");
  }
}

void selfTest() {
  digitalWrite(LED1_PIN, HIGH);
  delay(200);
  digitalWrite(LED1_PIN, LOW);
  delay(100);
  digitalWrite(LED2_PIN, LOW);
  delay(200);
  digitalWrite(LED2_PIN, HIGH);
  delay(100);
  tone(BUZZER_PIN, 1000, 200);
  delay(300);
  digitalWrite(LED2_PIN, HIGH); // locked on boot
}
