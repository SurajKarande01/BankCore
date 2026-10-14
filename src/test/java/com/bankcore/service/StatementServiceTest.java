package com.bankcore.service;

import com.bankcore.dto.response.MonthlyStatementResponse;
import com.bankcore.entity.Account;
import com.bankcore.entity.TransactionRecord;
import com.bankcore.entity.enums.AccountStatus;
import com.bankcore.entity.enums.AccountType;
import com.bankcore.entity.enums.TransactionType;
import com.bankcore.repository.AccountRepository;
import com.bankcore.repository.TransactionRepository;
import com.bankcore.service.impl.StatementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private StatementServiceImpl statementService;

    private Account mockAccount;

    @BeforeEach
    void setUp() {
        mockAccount = Account.builder()
                .id(1L)
                .accountNumber("AC1000000001")
                .accountHolderName("Suraj Karande")
                .balance(new BigDecimal("1200.00"))
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should generate accurate monthly statement with opening balance, debits, credits, and closing balance")
    void shouldGenerateAccurateMonthlyStatement() {
        // First transaction: Credit of 500 (Destination balance was 1500, so opening balance was 1000)
        TransactionRecord txn1 = TransactionRecord.builder()
                .id(101L)
                .transactionReference("TXN-001")
                .destinationAccountId(1L)
                .amount(new BigDecimal("500.00"))
                .transactionType(TransactionType.DEPOSIT)
                .destinationBalanceAfter(new BigDecimal("1500.00"))
                .description("Salary Credit")
                .createdAt(LocalDateTime.of(2026, 10, 5, 10, 0))
                .build();

        // Second transaction: Debit of 300 (Source balance became 1200)
        TransactionRecord txn2 = TransactionRecord.builder()
                .id(102L)
                .transactionReference("TXN-002")
                .sourceAccountId(1L)
                .amount(new BigDecimal("300.00"))
                .transactionType(TransactionType.TRANSFER)
                .sourceBalanceAfter(new BigDecimal("1200.00"))
                .description("Grocery Transfer")
                .createdAt(LocalDateTime.of(2026, 10, 15, 14, 30))
                .build();

        when(accountRepository.findByAccountNumber("AC1000000001")).thenReturn(Optional.of(mockAccount));
        when(transactionRepository.findStatementTransactions(eq(1L), any(), any()))
                .thenReturn(List.of(txn1, txn2));

        MonthlyStatementResponse statement = statementService.generateMonthlyStatement("AC1000000001", 2026, 10);

        assertNotNull(statement);
        assertEquals("AC1000000001", statement.getAccountNumber());
        assertEquals("Suraj Karande", statement.getAccountHolderName());
        assertEquals(2026, statement.getYear());
        assertEquals(10, statement.getMonth());

        // Opening = 1500 - 500 = 1000
        assertEquals(new BigDecimal("1000.00"), statement.getOpeningBalance());
        assertEquals(new BigDecimal("500.00"), statement.getTotalCredits());
        assertEquals(new BigDecimal("300.00"), statement.getTotalDebits());
        // Closing = 1000 + 500 - 300 = 1200
        assertEquals(new BigDecimal("1200.00"), statement.getClosingBalance());
        assertEquals(2, statement.getTransactionCount());

        // Check individual statement rows
        assertEquals("CREDIT", statement.getTransactions().get(0).getType());
        assertEquals("DEBIT", statement.getTransactions().get(1).getType());
    }

    @Test
    @DisplayName("Should handle month with no transactions cleanly")
    void shouldHandleEmptyMonth() {
        when(accountRepository.findByAccountNumber("AC1000000001")).thenReturn(Optional.of(mockAccount));
        when(transactionRepository.findStatementTransactions(eq(1L), any(), any()))
                .thenReturn(List.of());

        MonthlyStatementResponse statement = statementService.generateMonthlyStatement("AC1000000001", 2026, 10);

        assertNotNull(statement);
        assertEquals(new BigDecimal("1200.00"), statement.getOpeningBalance());
        assertEquals(BigDecimal.ZERO, statement.getTotalCredits());
        assertEquals(BigDecimal.ZERO, statement.getTotalDebits());
        assertEquals(new BigDecimal("1200.00"), statement.getClosingBalance());
        assertEquals(0, statement.getTransactionCount());
    }
}
