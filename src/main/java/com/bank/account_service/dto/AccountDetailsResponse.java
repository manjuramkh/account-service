package com.bank.account_service.dto;

import com.bank.account_service.domain.AccountStatus;
import com.bank.account_service.domain.AccountType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountDetailsResponse(
        @NotNull UUID accountId,
        @NotNull String accountNumber,
        @NotNull String ifscCode,
        @NotNull UUID customerId,
        @NotNull String accountHolderName,
        @NotNull AccountType accountType,
        @NotNull AccountStatus accountStatus,
        @NotNull Instant createdAt,
        @NotNull Instant updatedAt,
        @NotNull BigDecimal balance
) {}

