package com.bank.account_service.controller;


import com.bank.account_service.domain.Account;
import com.bank.account_service.dto.AccountResponse;
import com.bank.account_service.dto.CreateAccountRequest;
import com.bank.account_service.mapper.AccountMapper;
import com.bank.account_service.service.AccountService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    Logger logger = Logger.getLogger(AccountController.class.getName());

    public AccountController(AccountService accountService, AccountMapper accountMapper) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid  @RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(request);
        logger.info("Created account with number: " + account.getAccountNumber());
        return ResponseEntity.ok(accountMapper.toResponse(account));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<AccountResponse> accounts = accountService.getAllAccounts();
        logger.info("Retrieved all accounts");
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        Account account = accountService.getAccount(accountNumber);
        logger.info("Retrieved account number" + accountNumber);
        return ResponseEntity.ok(accountMapper.toResponse(account));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable UUID id) {
        Account account = accountService.getAccountById(id);
        logger.info("Retrieved account with UUID:" + id);
        return ResponseEntity.ok(accountMapper.toResponse(account));
    }

    @PostMapping("/{id}/debit")
    public ResponseEntity<AccountResponse> debit(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        BigDecimal amount = new BigDecimal(body.get("amount"));
        Account account = accountService.debit(id, amount, body.getOrDefault("reason", "DEBIT"));
        return ResponseEntity.ok(accountMapper.toResponse(account));
    }

    @PostMapping("/{id}/credit")
    public ResponseEntity<AccountResponse> credit(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        BigDecimal amount = new BigDecimal(body.get("amount"));
        Account account = accountService.credit(id, amount, body.getOrDefault("reason", "CREDIT"));
        return ResponseEntity.ok(accountMapper.toResponse(account));
    }
}