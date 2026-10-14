package com.bankcore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementItemResponse {
    private String transactionReference;
    private LocalDateTime timestamp;
    private String description;
    private String type; // "DEBIT" or "CREDIT"
    private BigDecimal amount;
    private BigDecimal runningBalance;
}
