package com.bankcore.controller;

import com.bankcore.dto.response.ApiResponse;
import com.bankcore.dto.response.MonthlyStatementResponse;
import com.bankcore.service.StatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/statements")
@RequiredArgsConstructor
@Tag(name = "Statements", description = "Monthly statement generation with ledger reconciliation")
public class StatementController {

    private final StatementService statementService;

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Generate monthly account statement", description = "Aggregates opening balance, debits, credits, and closing balance for the specified year and month")
    public ResponseEntity<ApiResponse<MonthlyStatementResponse>> getMonthlyStatement(
            @PathVariable String accountNumber,
            @Parameter(description = "Calendar year (e.g. 2026)") @RequestParam int year,
            @Parameter(description = "Calendar month 1-12 (e.g. 10 for October)") @RequestParam int month
    ) {
        MonthlyStatementResponse response = statementService.generateMonthlyStatement(accountNumber, year, month);
        return ResponseEntity.ok(ApiResponse.ok("Monthly statement generated", response));
    }
}
