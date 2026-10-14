package com.bankcore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyStatementResponse {
    private String accountNumber;
    private String accountHolderName;
    private int year;
    private int month;
    private BigDecimal openingBalance;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private BigDecimal closingBalance;
    private int transactionCount;
    private List<StatementItemResponse> transactions;
    private LocalDateTime generatedAt;
}
