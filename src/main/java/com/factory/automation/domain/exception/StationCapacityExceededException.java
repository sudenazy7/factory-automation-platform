package com.factory.automation.domain.exception;

public class StationCapacityExceededException extends BusinessRuleViolationException {
    public StationCapacityExceededException(String stationCode, int maxCapacity) {
        super(String.format("İstasyon [%s] maksimum kapasitesine (%d) ulaştı! Yeni üretim kalemi kabul edilemez.",
                stationCode, maxCapacity));
    }
}
