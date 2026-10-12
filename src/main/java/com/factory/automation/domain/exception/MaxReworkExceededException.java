package com.factory.automation.domain.exception;

public class MaxReworkExceededException extends BusinessRuleViolationException {
    public MaxReworkExceededException(String serialNumber, int currentReworkCount, int maxAllowed) {
        super(String.format("Üretim Kalemi [%s] için maksimum yeniden işlem (rework) sınırı aşıldı! (%d / %d). Ürün FAILED durumuna alınmalıdır.",
                serialNumber, currentReworkCount, maxAllowed));
    }
}
