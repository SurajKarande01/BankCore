package com.bankcore.service.impl;

import com.bankcore.dto.response.MonthlyStatementResponse;
import com.bankcore.dto.response.StatementItemResponse;
import com.bankcore.entity.Account;
import com.bankcore.entity.TransactionRecord;
import com.bankcore.exception.AccountNotFoundException;
import com.bankcore.exception.InvalidTransactionException;
import com.bankcore.repository.AccountRepository;
import com.bankcore.repository.TransactionRepository;
import com.bankcore.service.StatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementServiceImpl implements StatementService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public MonthlyStatementResponse generateMonthlyStatement(String accountNumber, int year, int month) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new InvalidTransactionException("Account number is required");
        }
        if (month < 1 || month > 12) {
            throw new InvalidTransactionException("Month must be between 1 and 12");
        }
        if (year < 2000 || year > 2100) {
            throw new InvalidTransactionException("Year must be a valid calendar year");
        }

        String cleanAccNum = accountNumber.trim();
        Account account = accountRepository.findByAccountNumber(cleanAccNum)
                .orElseThrow(() -> AccountNotFoundException.forAccountNumber(cleanAccNum));

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999999999);

        List<TransactionRecord> periodTxns = transactionRepository.findStatementTransactions(
                account.getId(), startOfMonth, endOfMonth
        );

        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        List<StatementItemResponse> items = new ArrayList<>();

        BigDecimal openingBalance = BigDecimal.ZERO;

        // Calculate opening balance from the earliest transaction of the month, or fallback to current balance
        if (!periodTxns.isEmpty()) {
            TransactionRecord firstTxn = periodTxns.get(0);
            boolean isFirstDebit = account.getId().equals(firstTxn.getSourceAccountId());
            if (isFirstDebit) {
                openingBalance = firstTxn.getSourceBalanceAfter() != null
                        ? firstTxn.getSourceBalanceAfter().add(firstTxn.getAmount())
                        : account.getBalance();
            } else {
                openingBalance = firstTxn.getDestinationBalanceAfter() != null
                        ? firstTxn.getDestinationBalanceAfter().subtract(firstTxn.getAmount())
                        : account.getBalance();
            }
        } else {
            // No activity in this month, opening and closing balances are current balance
            openingBalance = account.getBalance();
        }

        for (TransactionRecord txn : periodTxns) {
            boolean isDebit = account.getId().equals(txn.getSourceAccountId());
            String type = isDebit ? "DEBIT" : "CREDIT";
            BigDecimal runningBalance = isDebit ? txn.getSourceBalanceAfter() : txn.getDestinationBalanceAfter();

            if (isDebit) {
                totalDebits = totalDebits.add(txn.getAmount());
            } else {
                totalCredits = totalCredits.add(txn.getAmount());
            }

            items.add(StatementItemResponse.builder()
                    .transactionReference(txn.getTransactionReference())
                    .timestamp(txn.getCreatedAt())
                    .description(txn.getDescription())
                    .type(type)
                    .amount(txn.getAmount())
                    .runningBalance(runningBalance != null ? runningBalance : BigDecimal.ZERO)
                    .build());
        }

        BigDecimal closingBalance = openingBalance.add(totalCredits).subtract(totalDebits);

        log.info("Generated statement for account {} (Month: {}/{}) - Opening: {}, Debits: {}, Credits: {}, Closing: {}",
                cleanAccNum, month, year, openingBalance, totalDebits, totalCredits, closingBalance);

        return MonthlyStatementResponse.builder()
                .accountNumber(account.getAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .year(year)
                .month(month)
                .openingBalance(openingBalance)
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .closingBalance(closingBalance)
                .transactionCount(items.size())
                .transactions(items)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}

