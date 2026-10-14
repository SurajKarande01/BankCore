package com.bankcore.dto.response;

import com.bankcore.entity.TransactionRecord;
import com.bankcore.entity.enums.TransactionStatus;
import com.bankcore.entity.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private String transactionReference;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private TransactionType transactionType;
    private TransactionStatus status;
    private String description;
    private String initiatedBy;
    private BigDecimal sourceBalanceAfter;
    private BigDecimal destinationBalanceAfter;
    private LocalDateTime timestamp;

    public static TransactionResponse fromEntity(TransactionRecord txn) {
        return TransactionResponse.builder()
                .transactionReference(txn.getTransactionReference())
                .sourceAccountNumber(txn.getSourceAccountNumber())
                .destinationAccountNumber(txn.getDestinationAccountNumber())
                .amount(txn.getAmount())
                .transactionType(txn.getTransactionType())
                .status(txn.getStatus())
                .description(txn.getDescription())
                .initiatedBy(txn.getInitiatedBy())
                .sourceBalanceAfter(txn.getSourceBalanceAfter())
                .destinationBalanceAfter(txn.getDestinationBalanceAfter())
                .timestamp(txn.getCreatedAt())
                .build();
    }
}
