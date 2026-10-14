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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    /**
     * Executes an ACID-compliant fund transfer between two bank accounts.
     * 
     * Key Concurrency & Transactional Guarantees:
     * 1. Spring @Transactional(rollbackFor = Exception.class) guarantees all-or-nothing atomicity.
     *    A failure midway completely rolls back the transaction — zero partial debits.
     * 2. Pessimistic Write Locking (@Lock(PESSIMISTIC_WRITE) / SELECT FOR UPDATE) serializes
     *    concurrent balance updates on the same accounts, eliminating the Lost Update problem.
     * 3. Deterministic Lock Ordering: Locks are acquired in ascending order of Account Primary Key (ID).
     *    This mathematically prevents cyclic wait / database deadlocks during bidirectional transfers.
     */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public TransactionResponse transferFunds(FundTransferRequest request) {
        String sourceAccNum = request.getSourceAccountNumber().trim();
        String destAccNum = request.getDestinationAccountNumber().trim();
        BigDecimal amount = request.getAmount();

        if (sourceAccNum.equalsIgnoreCase(destAccNum)) {
            throw new InvalidTransactionException("Source and destination accounts cannot be identical");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be strictly greater than zero");
        }

        // 1. Fetch uncommitted account entities to retrieve their Primary Keys
        Account rawSource = accountRepository.findByAccountNumber(sourceAccNum)
                .orElseThrow(() -> AccountNotFoundException.forAccountNumber(sourceAccNum));
        Account rawDest = accountRepository.findByAccountNumber(destAccNum)
                .orElseThrow(() -> AccountNotFoundException.forAccountNumber(destAccNum));

        // 2. Deadlock Prevention via Deterministic Lock Ordering
        // Always acquire locks in ascending order of Account ID: min(id) first, then max(id)
        Long firstLockId = Math.min(rawSource.getId(), rawDest.getId());
        Long secondLockId = Math.max(rawSource.getId(), rawDest.getId());

        accountRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> AccountNotFoundException.forId(firstLockId));
        accountRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> AccountNotFoundException.forId(secondLockId));

        // 3. Re-read the locked instances with row-level locks engaged
        Account sourceAccount = accountRepository.findById(rawSource.getId())
                .orElseThrow(() -> AccountNotFoundException.forId(rawSource.getId()));
        Account destAccount = accountRepository.findById(rawDest.getId())
                .orElseThrow(() -> AccountNotFoundException.forId(rawDest.getId()));

        // 4. Validate account status
        if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Source account " + sourceAccNum + " is not ACTIVE. Current status: " + sourceAccount.getStatus());
        }
        if (destAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Destination account " + destAccNum + " is not ACTIVE. Current status: " + destAccount.getStatus());
        }

        // 5. Debit sender (throws InsufficientBalanceException if balance is inadequate, causing rollback)
        sourceAccount.debit(amount);
        // 6. Credit receiver
        destAccount.credit(amount);

        accountRepository.save(sourceAccount);
        accountRepository.save(destAccount);

        // 7. Generate unique transaction reference and record in ledger
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
                .initiatedBy(request.getInitiatedBy())
                .sourceBalanceAfter(sourceAccount.getBalance())
                .destinationBalanceAfter(destAccount.getBalance())
                .createdAt(LocalDateTime.now())
                .build();

        TransactionRecord savedTxn = transactionRepository.save(txnRecord);

        // 8. Log audit trail (who triggered it, debit amount, credit amount)
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

        account.debit(amount); // Throws InsufficientBalanceException if balance is insufficient
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
        TransactionRecord txn = transactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found for reference: " + transactionReference));
        return TransactionResponse.fromEntity(txn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentTransactions(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> AccountNotFoundException.forAccountNumber(accountNumber));

        return transactionRepository.findRecentTransactions(account.getId())
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
