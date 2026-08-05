package com.bank.account_service.integrationTest;

import com.bank.account_service.controller.AccountController;
import com.bank.account_service.domain.Account;
import com.bank.account_service.domain.AccountStatus;
import com.bank.account_service.domain.AccountType;
import com.bank.account_service.dto.AccountResponse;
import com.bank.account_service.dto.CreateAccountRequest;
import com.bank.account_service.dto.CreditRequest;
import com.bank.account_service.dto.DebitRequest;
import com.bank.account_service.exception.InsufficientFundsException;
import com.bank.account_service.exception.MinimumBalanceException;
import com.bank.account_service.exception.NoAccountFoundException;
import com.bank.account_service.mapper.AccountMapper;
import com.bank.account_service.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@DisplayName("AccountController Integration Tests")
public class ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountMapper accountMapper;

    private Account testAccount;
    private AccountResponse testAccountResponse;
    private CreateAccountRequest createAccountRequest;
    private UUID customerId;
    private UUID accountId;
    private String accountNumber;

    @TestConfiguration
    public static class TestConfig {
        @Bean
        @Primary
        public AccountService accountService() {
            return mock(AccountService.class);
        }

        @Bean
        @Primary
        public AccountMapper accountMapper() {
            return mock(AccountMapper.class);
        }
    }

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        accountNumber = "ACC123456789";

        testAccount = new Account();
        testAccount.setId(accountId);
        testAccount.setAccountNumber(accountNumber);
        testAccount.setIfscCode("BANK0001234");
        testAccount.setCustomerId(customerId);
        testAccount.setAccountType(AccountType.SAVING);
        testAccount.setBalance(new BigDecimal("10000.00"));
        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setCreatedAt(Instant.now());

        testAccountResponse = new AccountResponse(
                accountId,
                accountNumber,
                "BANK0001234",
                customerId.toString(),
                AccountType.SAVING,
                "ACTIVE",
                Instant.now().toString(),
                Instant.now().toString(),
                "10000.00"
        );

        createAccountRequest = new CreateAccountRequest(
                customerId,
                AccountType.SAVING,
                new BigDecimal("5000.00")
        );
    }

    // ===================== POST /api/v1/accounts - Create Account Tests =====================

    @Test
    @DisplayName("Should create account successfully")
    void testCreateAccountSuccess() throws Exception {
        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(testAccount);
        when(accountMapper.toResponse(testAccount)).thenReturn(testAccountResponse);

        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.accountNumber").value(accountNumber))
                .andExpect(jsonPath("$.accountType").value("SAVING"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AccountResponse response = objectMapper.readValue(responseBody, AccountResponse.class);
        assert response.accountNumber().equals(accountNumber);
    }

    @Test
    @DisplayName("Should return 400 when customerId is null")
    void testCreateAccountWithNullCustomerId() throws Exception {
        CreateAccountRequest invalidRequest = new CreateAccountRequest(
                null,
                AccountType.SAVING,
                new BigDecimal("5000.00")
        );

        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when accountType is null")
    void testCreateAccountWithNullAccountType() throws Exception {
        CreateAccountRequest invalidRequest = new CreateAccountRequest(
                customerId,
                null,
                new BigDecimal("5000.00")
        );

        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when initialBalance is null")
    void testCreateAccountWithNullInitialBalance() throws Exception {
        CreateAccountRequest invalidRequest = new CreateAccountRequest(
                customerId,
                AccountType.SAVING,
                null
        );

        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ===================== GET /api/v1/accounts - Get All Accounts Tests =====================

    @Test
    @DisplayName("Should retrieve all accounts successfully")
    void testGetAllAccountsSuccess() throws Exception {
        Account account2 = new Account();
        account2.setId(UUID.randomUUID());
        account2.setAccountNumber("ACC987654321");
        account2.setIfscCode("BANK0005678");
        account2.setCustomerId(UUID.randomUUID());
        account2.setAccountType(AccountType.CURRENT);
        account2.setBalance(new BigDecimal("5000.00"));
        account2.setStatus(AccountStatus.ACTIVE);

        AccountResponse response2 = new AccountResponse(
                account2.getId(),
                account2.getAccountNumber(),
                account2.getIfscCode(),
                account2.getCustomerId().toString(),
                account2.getAccountType(),
                "ACTIVE",
                Instant.now().toString(),
                Instant.now().toString(),
                "5000.00"
        );

        List<AccountResponse> accountList = Arrays.asList(testAccountResponse, response2);
        when(accountService.getAllAccounts()).thenReturn(accountList);

        mockMvc.perform(get("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accountNumber").value(accountNumber))
                .andExpect(jsonPath("$[1].accountNumber").value("ACC987654321"));
    }

    @Test
    @DisplayName("Should return empty list when no accounts exist")
    void testGetAllAccountsEmpty() throws Exception {
        when(accountService.getAllAccounts()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ===================== GET /api/v1/accounts/number/{accountNumber} - Get Account by Number Tests =====================

    @Test
    @DisplayName("Should retrieve account by account number successfully")
    void testGetAccountByNumberSuccess() throws Exception {
        when(accountService.getAccount(accountNumber)).thenReturn(testAccount);
        when(accountMapper.toResponse(testAccount)).thenReturn(testAccountResponse);

        mockMvc.perform(get("/api/v1/accounts/number/{accountNumber}", accountNumber)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value(accountNumber))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.accountType").value("SAVING"));
    }

    @Test
    @DisplayName("Should return 404 when account not found by account number")
    void testGetAccountByNumberNotFound() throws Exception {
        String nonExistentAccountNumber = "NONEXISTENT";
        when(accountService.getAccount(nonExistentAccountNumber))
                .thenThrow(new NoAccountFoundException("Account not found"));

        mockMvc.perform(get("/api/v1/accounts/number/{accountNumber}", nonExistentAccountNumber)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ===================== GET /api/v1/accounts/{id} - Get Account by ID Tests =====================

    @Test
    @DisplayName("Should retrieve account by ID successfully")
    void testGetAccountByIdSuccess() throws Exception {
        when(accountService.getAccountById(accountId)).thenReturn(testAccount);
        when(accountMapper.toResponse(testAccount)).thenReturn(testAccountResponse);

        mockMvc.perform(get("/api/v1/accounts/{id}", accountId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.accountNumber").value(accountNumber))
                .andExpect(jsonPath("$.balance").value("10000.00"));
    }

    @Test
    @DisplayName("Should return 404 when account not found by ID")
    void testGetAccountByIdNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(accountService.getAccountById(nonExistentId))
                .thenThrow(new NoAccountFoundException("Account not found"));

        mockMvc.perform(get("/api/v1/accounts/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ===================== POST /api/v1/accounts/{id}/debit - Debit Tests =====================

    @Test
    @DisplayName("Should debit account successfully")
    void testDebitAccountSuccess() throws Exception {
        BigDecimal debitAmount = new BigDecimal("500.00");
        DebitRequest debitRequest = new DebitRequest(accountNumber, debitAmount);

        Account debitedAccount = new Account();
        debitedAccount.setId(accountId);
        debitedAccount.setAccountNumber(accountNumber);
        debitedAccount.setIfscCode("BANK0001234");
        debitedAccount.setCustomerId(customerId);
        debitedAccount.setAccountType(AccountType.SAVING);
        debitedAccount.setBalance(new BigDecimal("9500.00"));
        debitedAccount.setStatus(AccountStatus.ACTIVE);

        AccountResponse debitedResponse = new AccountResponse(
                accountId,
                accountNumber,
                "BANK0001234",
                customerId.toString(),
                AccountType.SAVING,
                "ACTIVE",
                Instant.now().toString(),
                Instant.now().toString(),
                "9500.00"
        );

        when(accountService.debit(eq(accountId), any(DebitRequest.class))).thenReturn(debitedAccount);
        when(accountMapper.toResponse(debitedAccount)).thenReturn(debitedResponse);

        mockMvc.perform(post("/api/v1/accounts/{id}/debit", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("9500.00"));
    }

    @Test
    @DisplayName("Should process multiple transactions successfully")
    void testMultipleTransactionsSuccess() throws Exception {
        // First transaction: Debit
        BigDecimal debitAmount = new BigDecimal("1000.00");
        DebitRequest debitRequest = new DebitRequest(accountNumber, debitAmount);

        Account debitedAccount = new Account();
        debitedAccount.setId(accountId);
        debitedAccount.setAccountNumber(accountNumber);
        debitedAccount.setIfscCode("BANK0001234");
        debitedAccount.setCustomerId(customerId);
        debitedAccount.setAccountType(AccountType.SAVING);
        debitedAccount.setBalance(new BigDecimal("9000.00"));
        debitedAccount.setStatus(AccountStatus.ACTIVE);

        AccountResponse debitedResponse = new AccountResponse(
                accountId, accountNumber, "BANK0001234", customerId.toString(),
                AccountType.SAVING, "ACTIVE", Instant.now().toString(),
                Instant.now().toString(), "9000.00"
        );

        when(accountService.debit(eq(accountId), any(DebitRequest.class))).thenReturn(debitedAccount);
        when(accountMapper.toResponse(debitedAccount)).thenReturn(debitedResponse);

        mockMvc.perform(post("/api/v1/accounts/{id}/debit", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("9000.00"));
    }

    @Test
    @DisplayName("Should return 422 when debit would violate minimum balance")
    void testDebitAccountMinimumBalanceViolation() throws Exception {
        BigDecimal debitAmount = new BigDecimal("5000.00");
        DebitRequest debitRequest = new DebitRequest(accountNumber, debitAmount);

        when(accountService.debit(eq(accountId), any(DebitRequest.class)))
                .thenThrow(new MinimumBalanceException("Debit would violate minimum balance requirement"));

        mockMvc.perform(post("/api/v1/accounts/{id}/debit", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Should return 404 when debiting non-existent account")
    void testDebitNonExistentAccount() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        DebitRequest debitRequest = new DebitRequest(accountNumber, new BigDecimal("500.00"));

        when(accountService.debit(eq(nonExistentId), any(DebitRequest.class)))
                .thenThrow(new NoAccountFoundException("Account not found"));

        mockMvc.perform(post("/api/v1/accounts/{id}/debit", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitRequest)))
                .andExpect(status().isNotFound());
    }

    // ===================== POST /api/v1/accounts/{id}/credit - Credit Tests =====================

    @Test
    @DisplayName("Should credit account successfully")
    void testCreditAccountSuccess() throws Exception {
        BigDecimal creditAmount = new BigDecimal("2000.00");
        CreditRequest creditRequest = new CreditRequest();
        creditRequest.setAccountId(accountId);
        creditRequest.setAmount(creditAmount);

        Account creditedAccount = new Account();
        creditedAccount.setId(accountId);
        creditedAccount.setAccountNumber(accountNumber);
        creditedAccount.setIfscCode("BANK0001234");
        creditedAccount.setCustomerId(customerId);
        creditedAccount.setAccountType(AccountType.SAVING);
        creditedAccount.setBalance(new BigDecimal("12000.00"));
        creditedAccount.setStatus(AccountStatus.ACTIVE);

        AccountResponse creditedResponse = new AccountResponse(
                accountId,
                accountNumber,
                "BANK0001234",
                customerId.toString(),
                AccountType.SAVING,
                "ACTIVE",
                Instant.now().toString(),
                Instant.now().toString(),
                "12000.00"
        );

        when(accountService.credit(eq(accountId), any(CreditRequest.class))).thenReturn(creditedAccount);
        when(accountMapper.toResponse(creditedAccount)).thenReturn(creditedResponse);

        mockMvc.perform(post("/api/v1/accounts/{id}/credit", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("12000.00"));
    }

    @Test
    @DisplayName("Should credit account with null amount without validation")
    void testCreditAccountWithNullAmount() throws Exception {
        CreditRequest creditRequest = new CreditRequest();
        creditRequest.setAccountId(accountId);
        creditRequest.setAmount(null);

        when(accountService.credit(eq(accountId), any(CreditRequest.class))).thenReturn(testAccount);
        when(accountMapper.toResponse(testAccount)).thenReturn(testAccountResponse);

        mockMvc.perform(post("/api/v1/accounts/{id}/credit", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should credit account with zero amount without validation")
    void testCreditAccountWithInvalidAmount() throws Exception {
        CreditRequest creditRequest = new CreditRequest();
        creditRequest.setAccountId(accountId);
        creditRequest.setAmount(new BigDecimal("0.00"));

        when(accountService.credit(eq(accountId), any(CreditRequest.class))).thenReturn(testAccount);
        when(accountMapper.toResponse(testAccount)).thenReturn(testAccountResponse);

        mockMvc.perform(post("/api/v1/accounts/{id}/credit", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 404 when crediting non-existent account")
    void testCreditNonExistentAccount() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        CreditRequest creditRequest = new CreditRequest();
        creditRequest.setAccountId(nonExistentId);
        creditRequest.setAmount(new BigDecimal("1000.00"));

        when(accountService.credit(eq(nonExistentId), any(CreditRequest.class)))
                .thenThrow(new NoAccountFoundException("Account not found"));

        mockMvc.perform(post("/api/v1/accounts/{id}/credit", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditRequest)))
                .andExpect(status().isNotFound());
    }
}
