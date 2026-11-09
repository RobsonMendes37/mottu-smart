package com.mottu.ops.domain.exception;

public class DomainRuleException extends BusinessRuleException {

    private final String code;

    public DomainRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

