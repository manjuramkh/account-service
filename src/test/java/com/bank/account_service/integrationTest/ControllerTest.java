package com.bank.account_service.integrationTest;

import com.bank.account_service.controller.AccountController;
import com.bank.account_service.domain.Account;
import com.bank.account_service.domain.AccountStatus;
import com.bank.account_service.domain.AccountType;
import com.bank.account_service.dto.AccountResponse;
import com.bank.account_service.dto.CreateAccountRequest;
import com.bank.account_service.dto.CreditRequest;
import com.bank.account_service.dto.DebitRequest;
import com.bank.account_service.mapper.AccountMapper;
import com.bank.account_service.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(AccountController.class)
@Import(ControllerTest.TestSecurityConfiguration.class)
public class ControllerTest {

    private static final String BASE_URL = "/api/v1/accounts";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AccountService accountService;

    @MockBean
    private AccountMapper accountMapper;

    @Test
    void createAccountReturnsCreatedAccount() {
        UUID customerId = UUID.randomUUID();
        Account account = account(customerId);
        AccountResponse response = response(account);
        CreateAccountRequest request = new CreateAccountRequest(
                customerId, AccountType.SAVING, BigDecimal.valueOf(1000));
        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(response);

        webTestClient.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountId").isEqualTo(account.getId().toString())
                .jsonPath("$.accountNumber").isEqualTo(account.getAccountNumber())
                .jsonPath("$.balance").isEqualTo("1000");

        verify(accountService).createAccount(request);
    }

    @Test
    void createAccountRejectsMissingRequiredFields() {
        webTestClient.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getAllAccountsReturnsAccountList() {
        AccountResponse response = response(account(UUID.randomUUID()));
        when(accountService.getAllAccounts()).thenReturn(List.of(response));

        webTestClient.get().uri(BASE_URL)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].accountNumber").isEqualTo(response.accountNumber());

        verify(accountService).getAllAccounts();
    }

    @Test
    void getAccountByNumberReturnsAccount() {
        Account account = account(UUID.randomUUID());
        AccountResponse response = response(account);
        when(accountService.getAccount(account.getAccountNumber())).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(response);

        webTestClient.get().uri(BASE_URL + "/number/{accountNumber}", account.getAccountNumber())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountNumber").isEqualTo(account.getAccountNumber());

        verify(accountService).getAccount(account.getAccountNumber());
    }

    @Test
    void getAccountByIdReturnsAccount() {
        Account account = account(UUID.randomUUID());
        AccountResponse response = response(account);
        when(accountService.getAccountById(account.getId())).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(response);

        webTestClient.get().uri(BASE_URL + "/{id}", account.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountId").isEqualTo(account.getId().toString());

        verify(accountService).getAccountById(account.getId());
    }

    @Test
    void debitReturnsUpdatedAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = account(UUID.randomUUID());
        AccountResponse response = response(account);
        DebitRequest request = new DebitRequest(account.getAccountNumber(), BigDecimal.valueOf(100));
        when(accountService.debit(eq(accountId), any(DebitRequest.class))).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(response);

        webTestClient.post().uri(BASE_URL + "/{id}/debit", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountNumber").isEqualTo(account.getAccountNumber());

        verify(accountService).debit(eq(accountId), eq(request));
    }

    @Test
    void creditReturnsUpdatedAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = account(UUID.randomUUID());
        AccountResponse response = response(account);
        CreditRequest request = new CreditRequest();
        request.setAmount(BigDecimal.valueOf(100));
        when(accountService.credit(eq(accountId), any(CreditRequest.class))).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(response);

        webTestClient.post().uri(BASE_URL + "/{id}/credit", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountNumber").isEqualTo(account.getAccountNumber());

        verify(accountService).credit(eq(accountId), any(CreditRequest.class));
    }

    @Test
    void getAccountsByTypePassesTypeToService() {
        AccountResponse response = response(account(UUID.randomUUID()));
        when(accountService.getAccountByType(AccountType.SAVING)).thenReturn(List.of(response));

        webTestClient.get().uri(uriBuilder -> uriBuilder.path(BASE_URL + "/types")
                        .queryParam("accountType", AccountType.SAVING)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].accountType").isEqualTo(AccountType.SAVING.name());

        verify(accountService).getAccountByType(AccountType.SAVING);
    }

    private static Account account(UUID customerId) {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setAccountNumber("ACC123456789");
        account.setIfscCode("CNAB1234");
        account.setCustomerId(customerId);
        account.setAccountType(AccountType.SAVING);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(BigDecimal.valueOf(1000));
        account.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        return account;
    }

    private static AccountResponse response(Account account) {
        String createdAt = account.getCreatedAt().toString();
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getIfscCode(),
                account.getCustomerId().toString(),
                account.getAccountType(),
                account.getStatus().toString(),
                createdAt,
                createdAt,
                account.getBalance().toPlainString());
    }

    @TestConfiguration
    static class TestSecurityConfiguration {

        @Bean
        SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
            return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                    .build();
        }
    }

}
