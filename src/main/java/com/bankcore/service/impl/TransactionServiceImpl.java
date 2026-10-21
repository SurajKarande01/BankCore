package com.bankcore.service.impl;

import com.bankcore.dto.request.DepositRequest;
import com.bankcore.dto.request.FundTransferRequest;
import com.bankcore.dto.request.WithdrawRequest;
import com.bankcore.dto.response.TransactionResponse;
import com.bankcore.entity.Account;
import com.bankcore.entity.TransactionRecord;
import com.bankcore.entity.enums.AccountStatus;
import com.bankcore.entity.enums.TransactionStatus;
import com.bankcore.entity.enums.TransactionType;
import com.bankcore.exception.AccountInactiveException;
import com.bankcore.exception.AccountNotFoundException;
import com.bankcore.exception.InvalidTransactionException;
import com.bankcore.repository.AccountRepository;
import com.bankcore.repository.TransactionRepository;
import com.bankcore.service.AuditService;
import com.bankcore.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public TransactionResponse transferFunds(FundTransferRequest request) {
        if (request == null) {
            throw new InvalidTransactionException("Transfer request cannot be null");
        }

        if (request.getSourceAccountNumber() == null || request.getDestinationAccountNumber() == null) {
            throw new InvalidTransactionException("Source and destination account numbers are required");
        }

        String sourceAccNum = request.getSourceAccountNumber().trim();
        String destAccNum = request.getDestinationAccountNumber().trim();
        BigDecimal amount = request.getAmount();

        if (sourceAccNum.equalsIgnoreCase(destAccNum)) {
            throw new InvalidTransactionException("Source and destination accounts cannot be identical");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be strictly greater than zero");
        }

        Long sourceId = accountRepository.findIdByAccountNumber(sourceAccNum)
                .orElseThrow(() -> AccountNotFoundException.forAccountNumber(sourceAccNum));
        Long destId = accountRepository.findIdByAccountNumber(destAccNum)
                .orElseThrow(() -> AccountNotFoundException.forAccountNumber(destAccNum));

        // Always lock lower ID first to prevent deadlocks across concurrent transfers
        Long firstLockId = Math.min(sourceId, destId);
        Long secondLockId = Math.max(sourceId, destId);

        Account firstLocked = accountRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> AccountNotFoundException.forId(firstLockId));
        Account secondLocked = accountRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> AccountNotFoundException.forId(secondLockId));

        Account sourceAccount = sourceId.equals(firstLockId) ? firstLocked : secondLocked;
        Account destAccount = destId.equals(firstLockId) ? firstLocked : secondLocked;

        if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Source account " + sourceAccNum + " is not ACTIVE. Current status: " + sourceAccount.getStatus());
        }
        if (destAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Destination account " + destAccNum + " is not ACTIVE. Current status: " + destAccount.getStatus());
        }

        sourceAccount.debit(amount);
        destAccount.credit(amount);

        accountRepository.save(sourceAccount);
        accountRepository.save(destAccount);

        String txnRef = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        TransactionRecord txnRecord = TransactionRecord.builder()
                .transactionReference(txnRef)
                .sourceAccountId(sourceAccount.getId())
                .sourceAccountNumber(sourceAccount.getAccountNumber())
                .destinationAccountId(destAccount.getId())
                .destinationAccountNumber(destAccount.getAccountNumber())
                .amount(amount)
                .transactionType(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription() != null ? request.getDescription() : "Fund Transfer")
                .initiatedBy(request.getInitiatedBy() != null ? request.getInitiatedBy() : "SYSTEM")
                .sourceBalanceAfter(sourceAccount.getBalance())
                .destinationBalanceAfter(destAccount.getBalance())
                .createdAt(LocalDateTime.now())
                .build();

        TransactionRecord savedTxn = transactionRepository.save(txnRecord);

        auditService.log(
                "TRANSACTION",
                savedTxn.getId(),
                "FUND_TRANSFER",
                request.getInitiatedBy(),
                String.format("Transferred %s from %s to %s. TxnRef: %s", amount, sourceAccNum, destAccNum, txnRef),
                "SUCCESS",
                "127.0.0.1"
        );

        log.info("Successfully transferred {} from {} to {} [Ref: {}]", amount, sourceAccNum, destAccNum, txnRef);
        return TransactionResponse.fromEntity(savedTxn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public TransactionResponse deposit(DepositRequest request) {
        if (request == null || request.getAccountNumber() == null) {
            throw new InvalidTransactionException("Account number is required for deposit");
        }

        String accNum = request.getAccountNumber().trim();
        BigDecimal amount = request.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Deposit amount must be strictly greater than zero");
        }

        Account account = accountRepository.findByAccountNumberForUpdate(accNum)
                .orElseThrow(() -> AccountNotFoundException.forAccountNumber(accNum));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Account " + accNum + " is not ACTIVE");
        }

        account.credit(amount);
        accountRepository.save(account);

        String txnRef = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        TransactionRecord txnRecord = TransactionRecord.builder()
                .transactionReference(txnRef)
                .destinationAccountId(account.getId())
                .destinationAccountNumber(account.getAccountNumber())
                .amount(amount)
                .transactionType(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription() != null ? request.getDescription() : "Cash Deposit")
                .initiatedBy(request.getInitiatedBy() != null ? request.getInitiatedBy() : "TELLER")
                .destinationBalanceAfter(account.getBalance())
                .createdAt(LocalDateTime.now())
                .build();

        TransactionRecord savedTxn = transactionRepository.save(txnRecord);

        auditService.log(
                "TRANSACTION",
                savedTxn.getId(),
                "DEPOSIT",
                request.getInitiatedBy(),
                String.format("Deposited %s into %s. New Balance: %s", amount, accNum, account.getBalance()),
                "SUCCESS",
                "127.0.0.1"
        );

        return TransactionResponse.fromEntity(savedTxn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public TransactionResponse withdraw(WithdrawRequest request) {
        if (request == null || request.getAccountNumber() == null) {
            throw new InvalidTransactionException("Account number is required for withdrawal");
        }

        String accNum = request.getAccountNumber().trim();
        BigDecimal amount = request.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be strictly greater than zero");
        }

        Account account = accountRepository.findByAccountNumberForUpdate(accNum)
                .orElseThrow(() -> AccountNotFoundException.forAccountNumber(accNum));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Account " + accNum + " is not ACTIVE");
        }

        account.debit(amount);
        accountRepository.save(account);

        String txnRef = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        TransactionRecord txnRecord = TransactionRecord.builder()
                .transactionReference(txnRef)
                .sourceAccountId(account.getId())
                .sourceAccountNumber(account.getAccountNumber())
                .amount(amount)
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription() != null ? request.getDescription() : "Cash Withdrawal")
                .initiatedBy(request.getInitiatedBy() != null ? request.getInitiatedBy() : "ATM")
                .sourceBalanceAfter(account.getBalance())
                .createdAt(LocalDateTime.now())
                .build();

        TransactionRecord savedTxn = transactionRepository.save(txnRecord);

        auditService.log(
                "TRANSACTION",
                savedTxn.getId(),
                "WITHDRAWAL",
                request.getInitiatedBy(),
                String.format("Withdrew %s from %s. New Balance: %s", amount, accNum, account.getBalance()),
                "SUCCESS",
                "127.0.0.1"
        );

        return TransactionResponse.fromEntity(savedTxn);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String transactionReference) {
        if (transactionReference == null || transactionReference.trim().isEmpty()) {
            throw new InvalidTransactionException("Transaction reference is required");
        }
        String cleanRef = transactionReference.trim();
        TransactionRecord txn = transactionRepository.findByTransactionReference(cleanRef)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found for reference: " + cleanRef));
        return TransactionResponse.fromEntity(txn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentTransactions(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new InvalidTransactionException("Account number is required");
        }
        String cleanAccNum = accountNumber.trim();
        Account account = accountRepository.findByAccountNumber(cleanAccNum)
                .orElseThrow(() -> AccountNotFoundException.forAccountNumber(cleanAccNum));

        return transactionRepository.findRecentTransactions(account.getId())
                .stream()
                .map(TransactionResponse::fromEntity)
                .toList();
    }
}

