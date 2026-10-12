package com.factory.automation.domain.enums;

public enum WorkOrderPriority {
    LOW(1, "Düşük Öncelik"),
    MEDIUM(2, "Normal Öncelik"),
    HIGH(3, "Yüksek Öncelik"),
    URGENT(4, "Kritik / Acil");

    private final int level;
    private final String description;

    WorkOrderPriority(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }
}
