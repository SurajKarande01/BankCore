package com.bankcore.service.impl;

import com.bankcore.dto.request.CreateAccountRequest;
import com.bankcore.dto.response.AccountResponse;
import com.bankcore.entity.Account;
import com.bankcore.entity.TransactionRecord;
import com.bankcore.entity.enums.AccountStatus;
import com.bankcore.entity.enums.TransactionStatus;
import com.bankcore.entity.enums.TransactionType;
import com.bankcore.exception.AccountNotFoundException;
import com.bankcore.exception.InvalidTransactionException;
import com.bankcore.repository.AccountRepository;
import com.bankcore.repository.TransactionRepository;
import com.bankcore.service.AccountService;
import com.bankcore.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (request == null) {
            throw new InvalidTransactionException("Account request cannot be null");
        }

        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (email.isEmpty()) {
            throw new InvalidTransactionException("Email is required");
        }

        if (accountRepository.existsByEmail(email)) {
            throw new InvalidTransactionException("An account already exists with email: " + email);
        }

        String accountNumber = generateUniqueAccountNumber();
        BigDecimal initialBalance = request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO;
        String holderName = request.getAccountHolderName() != null ? request.getAccountHolderName().trim() : "";
        String phone = request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : "";

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountHolderName(holderName)
                .email(email)
                .phoneNumber(phone)
                .accountType(request.getAccountType())
                .status(AccountStatus.ACTIVE)
                .balance(initialBalance)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Account savedAccount = accountRepository.save(account);

        // Record opening balance in transactions ledger if funded
        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            String initialTxnRef = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            TransactionRecord initialTxn = TransactionRecord.builder()
                    .transactionReference(initialTxnRef)
                    .destinationAccountId(savedAccount.getId())
                    .destinationAccountNumber(savedAccount.getAccountNumber())
                    .amount(initialBalance)
                    .transactionType(TransactionType.DEPOSIT)
                    .status(TransactionStatus.SUCCESS)
                    .description("Initial opening balance deposit")
                    .initiatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "ACCOUNT_OPENING_AGENT")
                    .destinationBalanceAfter(initialBalance)
                    .createdAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(initialTxn);
        }

        auditService.log(
                "ACCOUNT",
                savedAccount.getId(),
                "CREATE_ACCOUNT",
                request.getCreatedBy() != null ? request.getCreatedBy() : "SYSTEM",
                String.format("Opened account %s for %s with initial deposit %s",
                        accountNumber, savedAccount.getAccountHolderName(), initialBalance),
                "SUCCESS",
                "127.0.0.1"
        );

        log.info("Created new bank account: {} for holder: {}", accountNumber, savedAccount.getAccountHolderName());
        return AccountResponse.fromEntity(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new InvalidTransactionException("Account number is required");
        }
        String cleanAccNum = accountNumber.trim();
        Account account = accountRepository.findByAccountNumber(cleanAccNum)
                .orElseThrow(() -> AccountNotFoundException.forAccountNumber(cleanAccNum));
        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        if (id == null) {
            throw new InvalidTransactionException("Account ID is required");
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> AccountNotFoundException.forId(id));
        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable).map(AccountResponse::fromEntity);
    }

    // Generate unique account number with bounded attempts to guarantee termination
    private String generateUniqueAccountNumber() {
        int maxAttempts = 15;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            long number = 1000000000L + (Math.abs(secureRandom.nextLong()) % 9000000000L);
            String candidate = "AC" + number;
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        return "AC" + (System.currentTimeMillis() % 10000000000L);
    }
}

