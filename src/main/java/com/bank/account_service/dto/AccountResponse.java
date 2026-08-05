package com.bank.account_service.dto;

import com.bank.account_service.domain.AccountType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AccountResponse(
    @NotNull UUID accountId,
    @NotNull String accountNumber,
    @NotNull String ifscCode,
    @NotNull String customerId,
    @NotNull AccountType accountType,
    @NotNull String accountStatus,
    @NotNull String createdAt,
    @NotNull String updatedAt,
    @NotNull String balance
) {}

