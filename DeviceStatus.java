package com.smartdoor.core;

public enum DeviceStatus {
    ONLINE  ("ONLINE",   "Device connected and operational"),
    STANDBY ("STANDBY",  "Device connected but idle"),
    OFFLINE ("OFFLINE",  "Device disconnected");

    private final String label;
    private final String description;

    DeviceStatus(String label, String description) {
        this.label       = label;
        this.description = description;
    }

    public String getLabel()       { return label; }
    public String getDescription() { return description; }
}
