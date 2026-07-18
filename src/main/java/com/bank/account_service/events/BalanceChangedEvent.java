package com.bank.account_service.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BalanceChangedEvent(
        UUID accountId,
        String accountNumber,
        BigDecimal previousBalance,
        BigDecimal newBalance,
        String reason,
        Instant occurredAt
) {}
