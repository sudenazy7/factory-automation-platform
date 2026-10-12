package com.factory.automation.domain.enums;

public enum WorkOrderStatus {
    CREATED("İş Emri Oluşturuldu"),
    IN_ASSEMBLY("Montaj Hattında"),
    QUALITY_CHECK("Kalite Testi Aşamasında"),
    COMPLETED("Tamamlandı"),
    FAILED("Başarısız / İptal Edildi"),
    HALTED("Acil Durduruldu");

    private final String description;

    WorkOrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    public boolean isOperational() {
        return this != HALTED && !isTerminal();
    }
}
