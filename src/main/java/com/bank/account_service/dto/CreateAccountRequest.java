package com.bank.account_service.dto;

import com.bank.account_service.domain.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(

    @NotNull
    UUID customerId,

    @NotNull
    AccountType accountType,

    @NotNull
    BigDecimal initialBalance

) {}
