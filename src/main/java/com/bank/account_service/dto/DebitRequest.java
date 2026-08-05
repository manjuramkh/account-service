package com.bank.account_service.dto;

import java.math.BigDecimal;

public record DebitRequest(
        String accountNumber,
//        String ifscCode,
//        String customerId,
        BigDecimal amount
)
{ }
