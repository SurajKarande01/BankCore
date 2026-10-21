package com.bankcore.entity;

import com.bankcore.entity.enums.AccountStatus;
import com.bankcore.entity.enums.AccountType;
import com.bankcore.exception.InsufficientBalanceException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "accounts",
    indexes = {
        @Index(name = "idx_account_number", columnList = "accountNumber", unique = true),
        @Index(name = "idx_account_email", columnList = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String accountNumber;

    @Column(nullable = false, length = 100)
    private String accountHolderName;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = AccountStatus.ACTIVE;
        }
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Deduct funds and verify sufficient balance
    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be strictly positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                String.format("Account %s has insufficient funds. Current balance: %s, requested debit: %s",
                    this.accountNumber, this.balance, amount)
            );
        }
        this.balance = this.balance.subtract(amount);
    }

    // Add funds to balance
    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be strictly positive");
        }
        this.balance = this.balance.add(amount);
    }
}
