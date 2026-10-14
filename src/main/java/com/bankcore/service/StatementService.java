package com.bankcore.service;

import com.bankcore.dto.response.MonthlyStatementResponse;

public interface StatementService {

    MonthlyStatementResponse generateMonthlyStatement(String accountNumber, int year, int month);
}
