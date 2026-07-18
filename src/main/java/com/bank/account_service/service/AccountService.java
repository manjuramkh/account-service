package com.bank.account_service.service;

import com.bank.account_service.client.CustomerServiceClient;
import com.bank.account_service.domain.Account;
import com.bank.account_service.dto.AccountResponse;
import com.bank.account_service.dto.CreateAccountRequest;
import com.bank.account_service.events.AccountEventProducer;
import com.bank.account_service.exception.InsufficientFundsException;
import com.bank.account_service.exception.MinimumBalanceException;
import com.bank.account_service.exception.NoAccountFoundException;
import com.bank.account_service.mapper.AccountMapper;
import com.bank.account_service.repos.AccountReportRepository;
import com.bank.account_service.repos.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountEventProducer eventProducer;
    private final CustomerServiceClient customerServiceClient;
    private final AccountReportRepository accountReportRepository;

    public AccountService(AccountRepository accountRepository, AccountEventProducer eventProducer, CustomerServiceClient customerServiceClient, AccountReportRepository accountReportRepository) {
        this.accountRepository = accountRepository;
        this.eventProducer = eventProducer;
        this.customerServiceClient = customerServiceClient;
        this.accountReportRepository = accountReportRepository;
    }

    @Transactional
    public Account createAccount(CreateAccountRequest request) {

        if (!customerServiceClient.isVerified(request.customerId())) {
            throw new NoSuchElementException("Customer not found: " + request.customerId());
        }

        if (request.initialBalance().compareTo(BigDecimal.valueOf(500)) < 0) {
            throw new MinimumBalanceException("Initial balance should be at least 500. Provided: " + request.initialBalance());
        }

        Account account = new Account();
        account.setCustomerId(request.customerId());
        account.setAccountNumber(UUID.randomUUID().toString().substring(0, 12));
        account.setAccountType(request.accountType());
        account.setBalance(request.initialBalance());
        account.setIfscCode("CNAB"+ ThreadLocalRandom.current().nextInt(1000, 10000)); // Set a default IFSC code

        return accountRepository.save(account);
    }

    @Transactional
    public List<AccountResponse> getAllAccounts() {
        List<Account> accounts =  accountRepository.findAll();
        return accounts.stream()
                .map(account -> new AccountMapper().toResponse(account))
                .toList();
    }

    @Transactional
    public Account debit(UUID accountId, BigDecimal amount, String reason) {
        // lock the row for the duration of this DB transaction
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(account.getAccountNumber());
        }

        BigDecimal previous = account.getBalance();
        account.setBalance(previous.subtract(amount));
        Account saved = accountRepository.save(account);

        // event fires after the DB write; a Kafka outbox pattern is safer for production
        // (guarantees the event isn't lost if the process crashes between save and publish)
//        eventProducer.publishBalanceChanged(new BalanceChangedEvent(
//                saved.getId(), saved.getAccountNumber(), previous, saved.getBalance(), reason, Instant.now()
//        ));

        return saved;
    }

    @Transactional
    public Account credit(UUID accountId, BigDecimal amount, String reason) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));

        BigDecimal previous = account.getBalance();
        account.setBalance(previous.add(amount));
        Account saved = accountRepository.save(account);

//        eventProducer.publishBalanceChanged(new BalanceChangedEvent(
//                saved.getId(), saved.getAccountNumber(), previous, saved.getBalance(), reason, Instant.now()
//        ));

        return saved;
    }

    public Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NoAccountFoundException("Account not found: " + accountNumber));
    }

    public Account getAccountById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new NoAccountFoundException("Account not found: " + id));
    }
}
