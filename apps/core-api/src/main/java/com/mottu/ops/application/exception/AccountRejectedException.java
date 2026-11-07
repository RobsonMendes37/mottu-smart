package com.mottu.ops.application.exception;

public class AccountRejectedException extends RuntimeException {
    public AccountRejectedException(String message) {
        super(message);
    }
}
