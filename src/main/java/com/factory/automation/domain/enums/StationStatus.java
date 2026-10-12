package com.factory.automation.domain.enums;

public enum StationStatus {
    IDLE("Boşta / Bekliyor"),
    ACTIVE("Aktif Çalışıyor"),
    MAINTENANCE("Bakımda"),
    HALTED("Acil Durduruldu"),
    ERROR("Hata Durumu");

    private final String description;

    StationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
