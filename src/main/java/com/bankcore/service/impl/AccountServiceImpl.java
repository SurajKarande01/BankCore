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
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new InvalidTransactionException("An account already exists with email: " + request.getEmail());
        }

        String accountNumber = generateUniqueAccountNumber();
        BigDecimal initialBalance = request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO;

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountHolderName(request.getAccountHolderName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phoneNumber(request.getPhoneNumber().trim())
                .accountType(request.getAccountType())
                .status(AccountStatus.ACTIVE)
                .balance(initialBalance)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Account savedAccount = accountRepository.save(account);

        // If there was an initial deposit > 0, record a transaction
        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            TransactionRecord initialTxn = TransactionRecord.builder()
                    .transactionReference("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
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

        // Write immutable audit log
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
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> AccountNotFoundException.forAccountNumber(accountNumber));
        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> AccountNotFoundException.forId(id));
        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable).map(AccountResponse::fromEntity);
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            long number = 1000000000L + (long) (secureRandom.nextDouble() * 9000000000L);
            accountNumber = "AC" + number;
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
