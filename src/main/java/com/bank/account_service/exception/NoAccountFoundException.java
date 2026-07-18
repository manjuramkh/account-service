package com.bank.account_service.exception;

public class NoAccountFoundException extends RuntimeException {
    public NoAccountFoundException(String msg) {
        super(msg);
    }
}
