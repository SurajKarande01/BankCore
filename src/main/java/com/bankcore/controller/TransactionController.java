package com.bankcore.controller;

import com.bankcore.dto.request.DepositRequest;
import com.bankcore.dto.request.FundTransferRequest;
import com.bankcore.dto.request.WithdrawRequest;
import com.bankcore.dto.response.ApiResponse;
import com.bankcore.dto.response.TransactionResponse;
import com.bankcore.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "ACID-compliant fund transfers, deposits, withdrawals, and ledger lookups")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds between accounts", description = "Executes an ACID-compliant fund transfer protected with pessimistic write locking and audit logging")
    public ResponseEntity<ApiResponse<TransactionResponse>> transferFunds(@Valid @RequestBody FundTransferRequest request) {
        TransactionResponse response = transactionService.transferFunds(request);
        return ResponseEntity.ok(ApiResponse.ok("Fund transfer completed successfully", response));
    }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit funds into an account")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(@Valid @RequestBody DepositRequest request) {
        TransactionResponse response = transactionService.deposit(request);
        return ResponseEntity.ok(ApiResponse.ok("Deposit completed successfully", response));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw funds from an account")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(@Valid @RequestBody WithdrawRequest request) {
        TransactionResponse response = transactionService.withdraw(request);
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal completed successfully", response));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Retrieve transaction details by unique reference")
    public ResponseEntity<ApiResponse<TransactionResponse>> getByReference(@PathVariable String reference) {
        TransactionResponse response = transactionService.getTransactionByReference(reference);
        return ResponseEntity.ok(ApiResponse.ok("Transaction retrieved", response));
    }

    @GetMapping("/account/{accountNumber}")
    @Operation(summary = "Get recent transaction history for an account")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getRecentTransactions(@PathVariable String accountNumber) {
        List<TransactionResponse> transactions = transactionService.getRecentTransactions(accountNumber);
        return ResponseEntity.ok(ApiResponse.ok("Recent transactions retrieved", transactions));
    }
}
