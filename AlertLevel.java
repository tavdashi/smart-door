package com.smartdoor.alerts;

public enum AlertLevel {
    INFO     ("INFO",     "ℹ️",  "Informational"),
    MEDIUM   ("MEDIUM",   "⚠️",  "Door open too long (10s)"),
    HIGH     ("HIGH",     "🔶", "Door open too long (20s)"),
    CRITICAL ("CRITICAL", "🚨", "Door open too long (35s)");

    private final String label;
    private final String icon;
    private final String description;

    AlertLevel(String label, String icon, String description) {
        this.label       = label;
        this.icon        = icon;
        this.description = description;
    }

    public String getLabel()       { return label; }
    public String getIcon()        { return icon; }
    public String getDescription() { return description; }
}
