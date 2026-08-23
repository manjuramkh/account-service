package com.bank.account_service.service;

import com.bank.account_service.domain.Account;
import com.bank.account_service.domain.AccountType;
import com.bank.account_service.dto.AccountResponse;
import com.bank.account_service.dto.CreateAccountRequest;
import com.bank.account_service.dto.CreditRequest;
import com.bank.account_service.dto.DebitRequest;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    public Account createAccount(CreateAccountRequest request);

    public Account getAccountById(UUID id);

    public List<AccountResponse> getAllAccounts();

    public Account debit(UUID accountId, DebitRequest debitRequest);

    public Account credit(UUID accountId, CreditRequest creditRequest);

    public Account getAccount(String accountNumber);

    List<AccountResponse> getAccountByType(AccountType accountType);
}
