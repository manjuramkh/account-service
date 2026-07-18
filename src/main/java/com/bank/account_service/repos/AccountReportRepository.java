package com.bank.account_service.repos;

import com.bank.account_service.domain.Account;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface AccountReportRepository extends PagingAndSortingRepository<Account, UUID> {


}
