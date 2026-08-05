package com.bank.account_service;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@OpenAPIDefinition(
        info = @Info(
                title = "Account Service API",
                version = "v1.0",
                description = "API for managing accounts in the banking system",
                contact = @Contact(
                        name = "Manjunath K H",
                        email = "khmanjunatha405@gmail.com"
                )
        ),
        externalDocs = @ExternalDocumentation(
                description = "Account service Documentation",
                url = "https://example.com/docs/account-service"
        )
)
@SpringBootApplication
//@EnableDiscoveryClient
public class AccountServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

}
