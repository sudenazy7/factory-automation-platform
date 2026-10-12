package com.factory.automation.domain.enums;

/**
 * Supported industry sectors for customized production pipelines,
 * validation rules, and telemetry thresholds.
 */
public enum IndustryType {
    AUTOMOTIVE("Otomotiv Üretimi"),
    ELECTRONICS("Elektronik & Yarı İletken"),
    FOOD_AND_BEVERAGE("Gıda ve İçecek"),
    PHARMACEUTICAL("İlaç ve Medikal"),
    GENERAL_MANUFACTURING("Genel İmalat Sanayi");

    private final String description;

    IndustryType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
