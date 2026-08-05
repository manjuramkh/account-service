package com.bank.account_service.mapper;

import com.bank.account_service.domain.Account;
import com.bank.account_service.dto.AccountResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getIfscCode(),
                account.getCustomerId().toString(),
                account.getAccountType(),
                account.getStatus().toString(),
                account.getCreatedAt() != null ? account.getCreatedAt().toString() : null,
                account.getCreatedAt() != null ? account.getCreatedAt().toString() : null,
                account.getBalance().toPlainString()
        );
    }
}
