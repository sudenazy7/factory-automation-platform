package com.factory.automation.domain.enums;

/**
 * Standard factory stations, extensible for industry-specific pipelines.
 */
public enum StationType {
    RAW_MATERIAL_ACCEPTANCE("Hammadde Kabul", 1),
    ASSEMBLY("Montaj / Kaynak", 2),
    PAINTING("Boyama & Kurutma", 3),
    QUALITY_CHECK("Kalite Kontrol", 4),
    PACKAGING("Paketleme & Sevkiyat", 5),
    CUSTOM("Özelleştirilmiş İstasyon", 99);

    private final String description;
    private final int defaultSequenceOrder;

    StationType(String description, int defaultSequenceOrder) {
        this.description = description;
        this.defaultSequenceOrder = defaultSequenceOrder;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultSequenceOrder() {
        return defaultSequenceOrder;
    }
}
