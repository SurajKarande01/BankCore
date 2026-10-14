package com.bankcore.service;

import com.bankcore.dto.request.CreateAccountRequest;
import com.bankcore.dto.response.AccountResponse;
import com.bankcore.entity.Account;
import com.bankcore.entity.enums.AccountStatus;
import com.bankcore.entity.enums.AccountType;
import com.bankcore.exception.AccountNotFoundException;
import com.bankcore.exception.InvalidTransactionException;
import com.bankcore.repository.AccountRepository;
import com.bankcore.repository.TransactionRepository;
import com.bankcore.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private CreateAccountRequest validRequest;
    private Account mockAccount;

    @BeforeEach
    void setUp() {
        validRequest = CreateAccountRequest.builder()
                .accountHolderName("Suraj Karande")
                .email("suraj@bankcore.com")
                .phoneNumber("+919876543210")
                .accountType(AccountType.SAVINGS)
                .initialDeposit(new BigDecimal("5000.00"))
                .createdBy("admin")
                .build();

        mockAccount = Account.builder()
                .id(1L)
                .accountNumber("AC1234567890")
                .accountHolderName("Suraj Karande")
                .email("suraj@bankcore.com")
                .phoneNumber("+919876543210")
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("5000.00"))
                .build();
    }

    @Test
    @DisplayName("Should create account successfully with initial deposit")
    void shouldCreateAccountSuccessfully() {
        when(accountRepository.existsByEmail("suraj@bankcore.com")).thenReturn(false);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        AccountResponse response = accountService.createAccount(validRequest);

        assertNotNull(response);
        assertEquals("AC1234567890", response.getAccountNumber());
        assertEquals("Suraj Karande", response.getAccountHolderName());
        assertEquals(new BigDecimal("5000.00"), response.getBalance());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());

        verify(accountRepository, times(1)).save(any(Account.class));
        verify(transactionRepository, times(1)).save(any());
        verify(auditService, times(1)).log(eq("ACCOUNT"), eq(1L), eq("CREATE_ACCOUNT"), anyString(), anyString(), eq("SUCCESS"), anyString());
    }

    @Test
    @DisplayName("Should reject account creation when email already exists")
    void shouldRejectDuplicateEmail() {
        when(accountRepository.existsByEmail("suraj@bankcore.com")).thenReturn(true);

        InvalidTransactionException ex = assertThrows(InvalidTransactionException.class, () ->
                accountService.createAccount(validRequest)
        );

        assertTrue(ex.getMessage().contains("already exists with email"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should fetch account by account number successfully")
    void shouldGetAccountByNumberSuccessfully() {
        when(accountRepository.findByAccountNumber("AC1234567890")).thenReturn(Optional.of(mockAccount));

        AccountResponse response = accountService.getAccountByNumber("AC1234567890");

        assertNotNull(response);
        assertEquals("AC1234567890", response.getAccountNumber());
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account does not exist")
    void shouldThrowWhenAccountNotFound() {
        when(accountRepository.findByAccountNumber("AC9999999999")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () ->
                accountService.getAccountByNumber("AC9999999999")
        );
    }
}
