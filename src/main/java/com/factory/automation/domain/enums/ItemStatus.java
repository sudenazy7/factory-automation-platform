package com.factory.automation.domain.enums;

public enum ItemStatus {
    CREATED("Üretim Kalemi Oluşturuldu"),
    IN_PROGRESS("İstasyonda İşleniyor"),
    PASSED("İstasyon Testinden Geçti"),
    REWORK("Yeniden İşlem (Rework) Gerekli"),
    FAILED("Kalite Kriterini Karşılayamadı (Hurda/Hata)"),
    PACKED("Paketlendi & Sevkiyata Hazır");

    private final String description;

    ItemStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
