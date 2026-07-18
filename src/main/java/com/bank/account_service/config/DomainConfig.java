package com.bank.account_service.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@Configuration
@EntityScan("com.bank.account_service")
@EnableJpaRepositories("com.bank.account_service")
@EnableTransactionManagement
public class DomainConfig {
}
