package com.bank.account_service.unitTest;

import com.bank.account_service.client.CustomerServiceClient;
import com.bank.account_service.domain.Account;
import com.bank.account_service.domain.AccountStatus;
import com.bank.account_service.domain.AccountType;
import com.bank.account_service.dto.AccountResponse;
import com.bank.account_service.dto.CreateAccountRequest;
import com.bank.account_service.dto.CreditRequest;
import com.bank.account_service.dto.DebitRequest;
import com.bank.account_service.events.AccountEventProducer;
import com.bank.account_service.exception.InsufficientFundsException;
import com.bank.account_service.exception.MinimumBalanceException;
import com.bank.account_service.exception.NoAccountFoundException;
import com.bank.account_service.repos.AccountReportRepository;
import com.bank.account_service.repos.AccountRepository;
import com.bank.account_service.service.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountEventProducer eventProducer;

    @Mock
    private CustomerServiceClient customerServiceClient;

    @Mock
    private AccountReportRepository accountReportRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private UUID customerId;
    private Account account;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        account = createAccount(customerId, BigDecimal.valueOf(1000));
    }

    @Test
    void createAccountReturnsSavedAccountWhenCustomerIsVerifiedAndBalanceMeetsMinimum() {
        CreateAccountRequest request = new CreateAccountRequest(
                customerId, AccountType.SAVING, BigDecimal.valueOf(1000));
        when(customerServiceClient.isVerified(customerId)).thenReturn(true);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            saved.setId(account.getId());
            return saved;
        });

        Account result = accountService.createAccount(request);

        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        assertEquals(AccountType.SAVING, result.getAccountType());
        assertEquals(BigDecimal.valueOf(1000), result.getBalance());
        assertEquals(12, result.getAccountNumber().length());
        verify(customerServiceClient).isVerified(customerId);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccountFailsWhenCustomerIsNotVerified() {
        CreateAccountRequest request = new CreateAccountRequest(
                customerId, AccountType.SAVING, BigDecimal.valueOf(1000));
        when(customerServiceClient.isVerified(customerId)).thenReturn(false);

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class, () -> accountService.createAccount(request));

        assertEquals("Customer not found: " + customerId, exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void createAccountFailsWhenInitialBalanceIsBelowMinimum() {
        CreateAccountRequest request = new CreateAccountRequest(
                customerId, AccountType.SAVING, BigDecimal.valueOf(499.99));
        when(customerServiceClient.isVerified(customerId)).thenReturn(true);

        assertThrows(MinimumBalanceException.class, () -> accountService.createAccount(request));

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getAllAccountsReturnsMappedResponses() {
        when(accountRepository.findAll()).thenReturn(List.of(account));

        List<AccountResponse> result = accountService.getAllAccounts();

        assertEquals(1, result.size());
        assertEquals(account.getId(), result.get(0).accountId());
        assertEquals(account.getAccountNumber(), result.get(0).accountNumber());
        assertEquals("1000", result.get(0).balance());
        verify(accountRepository).findAll();
    }

    @Test
    void getAllAccountsPropagatesRepositoryFailure() {
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(accountRepository.findAll()).thenThrow(failure);

        IllegalStateException result = assertThrows(
                IllegalStateException.class, () -> accountService.getAllAccounts());

        assertEquals(failure, result);
    }

    @Test
    void debitSubtractsAmountAndSavesAccount() {
        UUID accountId = account.getId();
        DebitRequest request = new DebitRequest(account.getAccountNumber(), BigDecimal.valueOf(200));
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        Account result = accountService.debit(accountId, request);

        assertEquals(BigDecimal.valueOf(800), result.getBalance());
        verify(accountRepository).save(account);
    }

    @Test
    void debitFailsWhenAccountDoesNotExist() {
        UUID accountId = UUID.randomUUID();
        DebitRequest request = new DebitRequest("MISSING", BigDecimal.valueOf(100));
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class, () -> accountService.debit(accountId, request));

        assertEquals("Account not found: " + accountId, exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void debitFailsWhenBalanceIsInsufficient() {
        DebitRequest request = new DebitRequest(account.getAccountNumber(), BigDecimal.valueOf(1001));
        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));

        assertThrows(InsufficientFundsException.class,
                () -> accountService.debit(account.getId(), request));

        assertEquals(BigDecimal.valueOf(1000), account.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void creditAddsAmountAndSavesAccount() {
        CreditRequest request = new CreditRequest();
        request.setAmount(BigDecimal.valueOf(250));
        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        Account result = accountService.credit(account.getId(), request);

        assertEquals(BigDecimal.valueOf(1250), result.getBalance());
        verify(accountRepository).save(account);
    }

    @Test
    void creditFailsWhenAccountDoesNotExist() {
        UUID accountId = UUID.randomUUID();
        CreditRequest request = new CreditRequest();
        request.setAmount(BigDecimal.valueOf(250));
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class, () -> accountService.credit(accountId, request));

        assertEquals("Account not found: " + accountId, exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getAccountReturnsMatchingAccount() {
        when(accountRepository.findByAccountNumber(account.getAccountNumber()))
                .thenReturn(Optional.of(account));

        Account result = accountService.getAccount(account.getAccountNumber());

        assertEquals(account, result);
        verify(accountRepository).findByAccountNumber(account.getAccountNumber());
    }

    @Test
    void getAccountFailsWhenAccountNumberIsUnknown() {
        String accountNumber = "UNKNOWN";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        NoAccountFoundException exception = assertThrows(
                NoAccountFoundException.class, () -> accountService.getAccount(accountNumber));

        assertEquals("Account not found: " + accountNumber, exception.getMessage());
    }

    @Test
    void getAccountByTypeReturnsMappedAccounts() {
        when(accountRepository.findByAccountType(AccountType.SAVING)).thenReturn(List.of(account));

        List<AccountResponse> result = accountService.getAccountByType(AccountType.SAVING);

        assertEquals(1, result.size());
        assertEquals(account.getId(), result.get(0).accountId());
        assertEquals(AccountType.SAVING, result.get(0).accountType());
        verify(accountRepository).findByAccountType(AccountType.SAVING);
    }

    @Test
    void getAccountByTypeFailsWhenNoAccountsExistForType() {
        when(accountRepository.findByAccountType(AccountType.CREDIT)).thenReturn(List.of());

        NoAccountFoundException exception = assertThrows(
                NoAccountFoundException.class,
                () -> accountService.getAccountByType(AccountType.CREDIT));

        assertEquals("No accounts found for type: CREDIT", exception.getMessage());
    }

    @Test
    void getAccountByIdReturnsMatchingAccount() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        Account result = accountService.getAccountById(account.getId());

        assertEquals(account, result);
        verify(accountRepository).findById(account.getId());
    }

    @Test
    void getAccountByIdFailsWhenAccountDoesNotExist() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        NoAccountFoundException exception = assertThrows(
                NoAccountFoundException.class, () -> accountService.getAccountById(accountId));

        assertEquals("Account not found: " + accountId, exception.getMessage());
    }

    private static Account createAccount(UUID customerId, BigDecimal balance) {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setCustomerId(customerId);
        account.setAccountNumber("ACC123456789");
        account.setIfscCode("CNAB1234");
        account.setAccountType(AccountType.SAVING);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(balance);
        account.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        return account;
    }
}
