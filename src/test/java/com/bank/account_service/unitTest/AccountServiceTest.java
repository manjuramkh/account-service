package com.bank.account_service.unitTest;

import com.bank.account_service.client.CustomerServiceClient;
import com.bank.account_service.domain.Account;
import com.bank.account_service.domain.AccountType;
import com.bank.account_service.dto.CreateAccountRequest;
import com.bank.account_service.events.AccountEventProducer;
import com.bank.account_service.exception.MinimumBalanceException;
import com.bank.account_service.repos.AccountReportRepository;
import com.bank.account_service.repos.AccountRepository;
import com.bank.account_service.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerServiceClient customerServiceClient;

    @Mock
    private AccountReportRepository accountReportRepository;

    @InjectMocks
    private AccountService accountService;

    private UUID customerId;
    private CreateAccountRequest validRequest;
    private Account mockAccount;

    @BeforeEach
    public void setUp() {
        customerId = UUID.randomUUID();
        validRequest = new CreateAccountRequest(
                customerId,
                AccountType.SAVING,
                BigDecimal.valueOf(1000)
        );

        mockAccount = new Account();
        mockAccount.setId(UUID.randomUUID());
        mockAccount.setCustomerId(customerId);
        mockAccount.setAccountNumber("ACC123456789");
        mockAccount.setAccountType(AccountType.SAVING);
        mockAccount.setBalance(BigDecimal.valueOf(1000));
        mockAccount.setIfscCode("CNAB1234");
    }

    @Test
    public void testCreateAccount_Success() {
        // Arrange
        when(customerServiceClient.isVerified(customerId)).thenReturn(true);
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        // Act
        Account result = accountService.createAccount(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        assertEquals(AccountType.SAVING, result.getAccountType());
        assertEquals(BigDecimal.valueOf(1000), result.getBalance());
        verify(customerServiceClient).isVerified(customerId);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    public void testCreateAccount_CustomerNotFound() {
        // Arrange
        when(customerServiceClient.isVerified(customerId)).thenReturn(false);

        // Act & Assert
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> accountService.createAccount(validRequest)
        );
        assertTrue(exception.getMessage().contains("Customer not found"));
        verify(customerServiceClient).isVerified(customerId);
        verify(accountRepository, never()).save(any());
    }

    @Test
    public void testCreateAccount_InsufficientBalance() {
        // Arrange
        CreateAccountRequest insufficientBalanceRequest = new CreateAccountRequest(
                customerId,
                AccountType.SAVING,
                BigDecimal.valueOf(400)
        );
        when(customerServiceClient.isVerified(customerId)).thenReturn(true);

        // Act & Assert
        MinimumBalanceException exception = assertThrows(
                MinimumBalanceException.class,
                () -> accountService.createAccount(insufficientBalanceRequest)
        );
        assertTrue(exception.getMessage().contains("Initial balance should be at least 500"));
        verify(customerServiceClient).isVerified(customerId);
        verify(accountRepository, never()).save(any());
    }

    @Test
    public void testCreateAccount_BalanceExactlyMinimum() {
        // Arrange
        CreateAccountRequest minimumBalanceRequest = new CreateAccountRequest(
                customerId,
                AccountType.SAVING,
                BigDecimal.valueOf(500)
        );
        when(customerServiceClient.isVerified(customerId)).thenReturn(true);
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        // Act
        Account result = accountService.createAccount(minimumBalanceRequest);

        // Assert
        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        verify(customerServiceClient).isVerified(customerId);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    public void testCreateAccount_DifferentAccountTypes() {
        // Arrange
        CreateAccountRequest creditAccountRequest = new CreateAccountRequest(
                customerId,
                AccountType.CREDIT,
                BigDecimal.valueOf(1500)
        );
        when(customerServiceClient.isVerified(customerId)).thenReturn(true);
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        // Act
        Account result = accountService.createAccount(creditAccountRequest);

        // Assert
        assertNotNull(result);
        verify(customerServiceClient).isVerified(customerId);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    public void testGetAllAccounts_ReturnsMappedResponses() {
        // Arrange
        Account account1 = new Account();
        account1.setId(UUID.randomUUID());
        account1.setAccountNumber("ACC1");
        account1.setIfscCode("IFSC1");
        account1.setCustomerId(customerId);
        account1.setAccountType(AccountType.SAVING);
        account1.setStatus(com.bank.account_service.domain.AccountStatus.ACTIVE);
        account1.setBalance(BigDecimal.valueOf(1000));

        Account account2 = new Account();
        account2.setId(UUID.randomUUID());
        account2.setAccountNumber("ACC2");
        account2.setIfscCode("IFSC2");
        account2.setCustomerId(customerId);
        account2.setAccountType(AccountType.CREDIT);
        account2.setStatus(com.bank.account_service.domain.AccountStatus.ACTIVE);
        account2.setBalance(BigDecimal.valueOf(1500));

        when(accountRepository.findAll()).thenReturn(java.util.List.of(account1, account2));

        // Act
        java.util.List<com.bank.account_service.dto.AccountResponse> responses = accountService.getAllAccounts();

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(account1.getAccountNumber(), responses.get(0).accountNumber());
        assertEquals(account2.getAccountNumber(), responses.get(1).accountNumber());
        verify(accountRepository).findAll();
    }

    @Test
    public void testDebit_Success() {
        // Arrange
        UUID accountId = mockAccount.getId();
        when(accountRepository.findByIdForUpdate(eq(accountId))).thenReturn(java.util.Optional.of(mockAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.bank.account_service.dto.DebitRequest debitRequest = new com.bank.account_service.dto.DebitRequest(mockAccount.getAccountNumber(), BigDecimal.valueOf(200));

        // Act
        Account result = accountService.debit(accountId, debitRequest);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(800), result.getBalance());
        verify(accountRepository).findByIdForUpdate(eq(accountId));
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    public void testDebit_InsufficientFunds() {
        // Arrange
        Account lowBalance = new Account();
        lowBalance.setId(mockAccount.getId());
        lowBalance.setAccountNumber(mockAccount.getAccountNumber());
        lowBalance.setBalance(BigDecimal.valueOf(100));

        UUID accountId = lowBalance.getId();
        when(accountRepository.findByIdForUpdate(eq(accountId))).thenReturn(java.util.Optional.of(lowBalance));

        com.bank.account_service.dto.DebitRequest debitRequest = new com.bank.account_service.dto.DebitRequest(lowBalance.getAccountNumber(), BigDecimal.valueOf(200));

        // Act & Assert
        com.bank.account_service.exception.InsufficientFundsException ex = assertThrows(
                com.bank.account_service.exception.InsufficientFundsException.class,
                () -> accountService.debit(accountId, debitRequest)
        );
        assertTrue(ex.getMessage().contains(lowBalance.getAccountNumber()));
        verify(accountRepository).findByIdForUpdate(eq(accountId));
        verify(accountRepository, never()).save(any());
    }

    @Test
    public void testDebit_AccountNotFound() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdForUpdate(eq(accountId))).thenReturn(java.util.Optional.empty());

        com.bank.account_service.dto.DebitRequest debitRequest = new com.bank.account_service.dto.DebitRequest("NONEXIST", BigDecimal.valueOf(50));

        // Act & Assert
        NoSuchElementException ex = assertThrows(
                NoSuchElementException.class,
                () -> accountService.debit(accountId, debitRequest)
        );
        assertTrue(ex.getMessage().contains("Account not found"));
        verify(accountRepository).findByIdForUpdate(eq(accountId));
        verify(accountRepository, never()).save(any());
    }

    @Test
    public void testCredit_Success() {
        // Arrange
        UUID accountId = mockAccount.getId();
        when(accountRepository.findByIdForUpdate(eq(accountId))).thenReturn(java.util.Optional.of(mockAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.bank.account_service.dto.CreditRequest creditRequest = new com.bank.account_service.dto.CreditRequest();
        creditRequest.setAmount(BigDecimal.valueOf(500));

        // Act
        Account result = accountService.credit(accountId, creditRequest);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1500), result.getBalance());
        verify(accountRepository).findByIdForUpdate(eq(accountId));
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    public void testCredit_AccountNotFound() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdForUpdate(eq(accountId))).thenReturn(java.util.Optional.empty());

        com.bank.account_service.dto.CreditRequest creditRequest = new com.bank.account_service.dto.CreditRequest();
        creditRequest.setAmount(BigDecimal.valueOf(100));

        // Act & Assert
        NoSuchElementException ex = assertThrows(
                NoSuchElementException.class,
                () -> accountService.credit(accountId, creditRequest)
        );
        assertTrue(ex.getMessage().contains("Account not found"));
        verify(accountRepository).findByIdForUpdate(eq(accountId));
        verify(accountRepository, never()).save(any());
    }

    @Test
    public void testGetAccount_Success() {
        // Arrange
        String accNum = mockAccount.getAccountNumber();
        when(accountRepository.findByAccountNumber(eq(accNum))).thenReturn(java.util.Optional.of(mockAccount));

        // Act
        Account result = accountService.getAccount(accNum);

        // Assert
        assertNotNull(result);
        assertEquals(accNum, result.getAccountNumber());
        verify(accountRepository).findByAccountNumber(eq(accNum));
    }

    @Test
    public void testGetAccount_NotFound() {
        // Arrange
        String accNum = "NOACC";
        when(accountRepository.findByAccountNumber(eq(accNum))).thenReturn(java.util.Optional.empty());

        // Act & Assert
        com.bank.account_service.exception.NoAccountFoundException ex = assertThrows(
                com.bank.account_service.exception.NoAccountFoundException.class,
                () -> accountService.getAccount(accNum)
        );
        assertTrue(ex.getMessage().contains("Account not found"));
        verify(accountRepository).findByAccountNumber(eq(accNum));
    }

    @Test
    public void testGetAccountById_Success() {
        // Arrange
        UUID id = mockAccount.getId();
        when(accountRepository.findById(eq(id))).thenReturn(java.util.Optional.of(mockAccount));

        // Act
        Account result = accountService.getAccountById(id);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(accountRepository).findById(eq(id));
    }

    @Test
    public void testGetAccountById_NotFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(eq(id))).thenReturn(java.util.Optional.empty());

        // Act & Assert
        com.bank.account_service.exception.NoAccountFoundException ex = assertThrows(
                com.bank.account_service.exception.NoAccountFoundException.class,
                () -> accountService.getAccountById(id)
        );
        assertTrue(ex.getMessage().contains("Account not found"));
        verify(accountRepository).findById(eq(id));
    }

}
