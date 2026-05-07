package com.hospital.queue.domain.enums;

public enum DoctorStatus {
    AVAILABLE("Available", "🟢"),
    BUSY("With Patient", "🔴"),
    ON_BREAK("On Break", "🟡"),
    OFFLINE("Offline", "⚫");

    private final String label;
    private final String icon;

    DoctorStatus(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getIcon()  { return icon;  }
}
