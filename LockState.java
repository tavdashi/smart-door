package com.smartdoor.lock;

public enum LockState {
    LOCKED   ("🔒 LOCKED",   "Door is locked"),
    UNLOCKED ("🔓 UNLOCKED", "Door is unlocked");

    private final String label;
    private final String description;

    LockState(String label, String description) {
        this.label       = label;
        this.description = description;
    }

    public String getLabel()       { return label; }
    public String getDescription() { return description; }
}
