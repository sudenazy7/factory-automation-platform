package com.factory.automation.domain.exception;

public class WorkOrderHaltedException extends BusinessRuleViolationException {
    public WorkOrderHaltedException(String orderNumber, String reason) {
        super(String.format("İş Emri [%s] HALTED (Durduruldu) durumunda! Yeni işlem veya durum güncellemesi yapılamaz. Durdurulma Nedeni: %s",
                orderNumber, reason));
    }
}
