package com.smartdoor.storage;

import com.smartdoor.model.EventType;
import com.smartdoor.model.SensorEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EventLog — Thread-Safe In-Memory Event Store
 *
 * Stores all SensorEvents in an ArrayList.
 * All methods are synchronized — safe for concurrent access from
 * sensor thread, alert thread, and display thread simultaneously.
 *
 * In production, this would write to a database (SQLite / Firebase).
 *
 * Demonstrates: Collections, Synchronization, Stream API
 */
public class EventLog {

    private final List<SensorEvent> events = new ArrayList<>();
    private static final int MAX_EVENTS = 500;

    // ── Add Event ─────────────────────────────────────────────────
    public synchronized void addEvent(SensorEvent event) {
        events.add(event);
        if (events.size() > MAX_EVENTS) {
            events.remove(0); // keep log bounded
        }
    }

    // ── Query Methods ─────────────────────────────────────────────

    public synchronized List<SensorEvent> getAllEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public synchronized List<SensorEvent> getLastN(int n) {
        int size  = events.size();
        int start = Math.max(0, size - n);
        return new ArrayList<>(events.subList(start, size));
    }

    public synchronized List<SensorEvent> getEventsByType(EventType type) {
        return events.stream()
            .filter(e -> e.getType() == type)
            .collect(Collectors.toList());
    }

    public synchronized List<SensorEvent> getAlertEvents() {
        return events.stream()
            .filter(e -> e.getType() == EventType.ALERT_DOOR_OPEN_TOO_LONG
                      || e.getType() == EventType.ALERT_UNLOCK_WHILE_OPEN)
            .collect(Collectors.toList());
    }

    public synchronized int size() { return events.size(); }

    public synchronized void clear() { events.clear(); }

    // ── Print Full Log ────────────────────────────────────────────
    public synchronized void printAll() {
        System.out.println("\n══════════════════════ FULL EVENT LOG ══════════════════════");
        if (events.isEmpty()) {
            System.out.println("  (no events recorded)");
        } else {
            events.forEach(e -> System.out.println("  " + e.toLogLine()));
        }
        System.out.println("════════════════════════════════════════════════════════════\n");
    }
}
