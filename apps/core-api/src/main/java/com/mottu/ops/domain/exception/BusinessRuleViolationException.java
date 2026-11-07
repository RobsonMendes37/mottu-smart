package com.mottu.ops.domain.exception;

public class BusinessRuleViolationException extends BusinessRuleException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
