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

}
