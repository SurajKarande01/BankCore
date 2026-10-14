package com.bankcore.controller;

import com.bankcore.dto.request.CreateAccountRequest;
import com.bankcore.dto.response.AccountResponse;
import com.bankcore.dto.response.ApiResponse;
import com.bankcore.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account opening, status checks, and balance inquiries")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Create a new bank account", description = "Opens a new SAVINGS or CURRENT account with initial deposit")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return new ResponseEntity<>(ApiResponse.ok("Account successfully created", response), HttpStatus.CREATED);
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Retrieve account details by account number")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable String accountNumber) {
        AccountResponse response = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(ApiResponse.ok("Account details retrieved", response));
    }

    @GetMapping
    @Operation(summary = "Get paginated list of accounts")
    public ResponseEntity<ApiResponse<Page<AccountResponse>>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Page<AccountResponse> accounts = accountService.getAllAccounts(PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.ok("Accounts retrieved", accounts));
    }
}
