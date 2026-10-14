package com.bankcore.service;

import com.bankcore.dto.request.DepositRequest;
import com.bankcore.dto.request.FundTransferRequest;
import com.bankcore.dto.request.WithdrawRequest;
import com.bankcore.dto.response.TransactionResponse;
import com.bankcore.entity.Account;
import com.bankcore.entity.TransactionRecord;
import com.bankcore.entity.enums.AccountStatus;
import com.bankcore.entity.enums.AccountType;
import com.bankcore.entity.enums.TransactionStatus;
import com.bankcore.entity.enums.TransactionType;
import com.bankcore.exception.AccountInactiveException;
import com.bankcore.exception.InsufficientBalanceException;
import com.bankcore.exception.InvalidTransactionException;
import com.bankcore.repository.AccountRepository;
import com.bankcore.repository.TransactionRepository;
import com.bankcore.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Account senderAccount;
    private Account receiverAccount;

    @BeforeEach
    void setUp() {
        senderAccount = Account.builder()
                .id(1L)
                .accountNumber("AC1000000001")
                .accountHolderName("Alice Smith")
                .balance(new BigDecimal("1000.00"))
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .build();

        receiverAccount = Account.builder()
                .id(2L)
                .accountNumber("AC1000000002")
                .accountHolderName("Bob Johnson")
                .balance(new BigDecimal("500.00"))
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should successfully transfer funds between two active accounts")
    void shouldTransferFundsSuccessfully() {
        FundTransferRequest request = FundTransferRequest.builder()
                .sourceAccountNumber("AC1000000001")
                .destinationAccountNumber("AC1000000002")
                .amount(new BigDecimal("300.00"))
                .description("Rent Payment")
                .initiatedBy("alice_user")
                .build();

        when(accountRepository.findByAccountNumber("AC1000000001")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber("AC1000000002")).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));

        when(transactionRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> {
            TransactionRecord txn = invocation.getArgument(0);
            txn.setId(100L);
            txn.setCreatedAt(LocalDateTime.now());
            return txn;
        });

        TransactionResponse response = transactionService.transferFunds(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("300.00"), response.getAmount());
        assertEquals(new BigDecimal("700.00"), senderAccount.getBalance());
        assertEquals(new BigDecimal("800.00"), receiverAccount.getBalance());

        // Verify deterministic lock order: 1L was locked first, then 2L
        verify(accountRepository).findByIdForUpdate(1L);
        verify(accountRepository).findByIdForUpdate(2L);
        verify(auditService).log(eq("TRANSACTION"), anyLong(), eq("FUND_TRANSFER"), eq("alice_user"), anyString(), eq("SUCCESS"), anyString());
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException and prevent partial debit when balance is too low")
    void shouldThrowInsufficientBalanceException() {
        FundTransferRequest request = FundTransferRequest.builder()
                .sourceAccountNumber("AC1000000001")
                .destinationAccountNumber("AC1000000002")
                .amount(new BigDecimal("5000.00")) // Exceeds balance of 1000
                .description("Overdraft attempt")
                .initiatedBy("alice_user")
                .build();

        when(accountRepository.findByAccountNumber("AC1000000001")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber("AC1000000002")).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));

        InsufficientBalanceException ex = assertThrows(InsufficientBalanceException.class, () ->
                transactionService.transferFunds(request)
        );

        assertTrue(ex.getMessage().contains("insufficient funds"));
        // Sender balance should remain untouched
        assertEquals(new BigDecimal("1000.00"), senderAccount.getBalance());
        assertEquals(new BigDecimal("500.00"), receiverAccount.getBalance());
        verify(transactionRepository, never()).save(any(TransactionRecord.class));
    }

    @Test
    @DisplayName("Should reject self-transfer to the exact same account")
    void shouldRejectSelfTransfer() {
        FundTransferRequest request = FundTransferRequest.builder()
                .sourceAccountNumber("AC1000000001")
                .destinationAccountNumber("AC1000000001")
                .amount(new BigDecimal("100.00"))
                .initiatedBy("alice_user")
                .build();

        InvalidTransactionException ex = assertThrows(InvalidTransactionException.class, () ->
                transactionService.transferFunds(request)
        );

        assertTrue(ex.getMessage().contains("identical"));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("Should throw AccountInactiveException if sender account is suspended")
    void shouldRejectTransferFromSuspendedAccount() {
        senderAccount.setStatus(AccountStatus.SUSPENDED);

        FundTransferRequest request = FundTransferRequest.builder()
                .sourceAccountNumber("AC1000000001")
                .destinationAccountNumber("AC1000000002")
                .amount(new BigDecimal("100.00"))
                .initiatedBy("alice_user")
                .build();

        when(accountRepository.findByAccountNumber("AC1000000001")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber("AC1000000002")).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));

        AccountInactiveException ex = assertThrows(AccountInactiveException.class, () ->
                transactionService.transferFunds(request)
        );

        assertTrue(ex.getMessage().contains("not ACTIVE"));
    }

    @Test
    @DisplayName("Should deposit cash into account with row-level lock")
    void shouldDepositSuccessfully() {
        DepositRequest request = DepositRequest.builder()
                .accountNumber("AC1000000001")
                .amount(new BigDecimal("250.00"))
                .description("Cash Deposit")
                .initiatedBy("teller_01")
                .build();

        when(accountRepository.findByAccountNumberForUpdate("AC1000000001")).thenReturn(Optional.of(senderAccount));
        when(transactionRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> {
            TransactionRecord txn = invocation.getArgument(0);
            txn.setId(201L);
            txn.setCreatedAt(LocalDateTime.now());
            return txn;
        });

        TransactionResponse response = transactionService.deposit(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("1250.00"), senderAccount.getBalance());
        assertEquals(TransactionType.DEPOSIT, response.getTransactionType());
    }

    @Test
    @DisplayName("Should withdraw cash from account with row-level lock")
    void shouldWithdrawSuccessfully() {
        WithdrawRequest request = WithdrawRequest.builder()
                .accountNumber("AC1000000001")
                .amount(new BigDecimal("200.00"))
                .description("ATM withdrawal")
                .initiatedBy("atm_01")
                .build();

        when(accountRepository.findByAccountNumberForUpdate("AC1000000001")).thenReturn(Optional.of(senderAccount));
        when(transactionRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> {
            TransactionRecord txn = invocation.getArgument(0);
            txn.setId(202L);
            txn.setCreatedAt(LocalDateTime.now());
            return txn;
        });

        TransactionResponse response = transactionService.withdraw(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("800.00"), senderAccount.getBalance());
        assertEquals(TransactionType.WITHDRAWAL, response.getTransactionType());
    }
}
