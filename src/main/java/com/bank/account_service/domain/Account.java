package com.bank.account_service.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Data
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false, unique = true, length = 11)
    private String ifscCode;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID customerId;  // Assuming customerId is a UUID referencing a customer entity

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

/**
    Optimistic locking version field:
    This field is used by JPA to implement optimistic locking.
    When an entity is updated, the version number is incremented.
    If two transactions try to update the same entity simultaneously,
    the one with the outdated version will fail, preventing lost updates.

    */

    @Version
    private Long version;

    @Column
    private Instant createdAt;

    @Column(name = "closed_date")
    private Instant closedDate;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.status = AccountStatus.ACTIVE;
    }

}
