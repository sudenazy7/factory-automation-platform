package com.factory.automation.domain.exception;

public class StationUnavailableException extends BusinessRuleViolationException {
    public StationUnavailableException(String stationCode, String stationStatus) {
        super(String.format("İstasyon [%s] işlem için uygun değil! Mevcut Durum: %s", stationCode, stationStatus));
    }
}
