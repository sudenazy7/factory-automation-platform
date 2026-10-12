package com.factory.automation.domain.exception;

public class InvalidStateTransitionException extends BusinessRuleViolationException {
    public InvalidStateTransitionException(String entityName, String currentState, String targetState, String reason) {
        super(String.format("Geçersiz durum geçişi [%s]: '%s' durumundan '%s' durumuna geçilemez. Gerekçe: %s",
                entityName, currentState, targetState, reason));
    }
}
