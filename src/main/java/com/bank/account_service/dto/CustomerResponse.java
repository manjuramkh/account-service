package com.bank.account_service.dto;

public record CustomerResponse(
        String customerId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber
) {
}
