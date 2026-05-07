package com.hospital.queue.domain.enums;

public enum Priority {
    CRITICAL(1, "🔴"),
    HIGH(2, "🟠"),
    NORMAL(3, "🟢"),
    LOW(4, "⚪");

    private final int order;
    private final String icon;

    Priority(int order, String icon) {
        this.order = order;
        this.icon = icon;
    }

    public int getOrder() { return order; }
    public String getIcon() { return icon; }
}
