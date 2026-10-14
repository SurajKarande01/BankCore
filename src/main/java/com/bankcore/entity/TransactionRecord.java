package com.bankcore.entity;

import com.bankcore.entity.enums.TransactionStatus;
import com.bankcore.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_txn_reference", columnList = "transactionReference", unique = true),
        @Index(name = "idx_txn_source_account", columnList = "sourceAccountId, createdAt"),
        @Index(name = "idx_txn_dest_account", columnList = "destinationAccountId, createdAt"),
        @Index(name = "idx_txn_created_at", columnList = "createdAt")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String transactionReference;

    private Long sourceAccountId;
    private String sourceAccountNumber;

    private Long destinationAccountId;
    private String destinationAccountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 100)
    private String initiatedBy;

    @Column(precision = 15, scale = 2)
    private BigDecimal sourceBalanceAfter;

    @Column(precision = 15, scale = 2)
    private BigDecimal destinationBalanceAfter;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = TransactionStatus.SUCCESS;
        }
    }
}
