package com.bank.account_service.exception;

public class MinimumBalanceException extends RuntimeException {
    public MinimumBalanceException(String accountNumber) {
        super("Minimum balance requirement to open an account: " + accountNumber);
    }
}
