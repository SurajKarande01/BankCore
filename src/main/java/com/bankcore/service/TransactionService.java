package com.bankcore.service;

import com.bankcore.dto.request.DepositRequest;
import com.bankcore.dto.request.FundTransferRequest;
import com.bankcore.dto.request.WithdrawRequest;
import com.bankcore.dto.response.TransactionResponse;

import java.util.List;

public interface TransactionService {

    TransactionResponse transferFunds(FundTransferRequest request);

    TransactionResponse deposit(DepositRequest request);

    TransactionResponse withdraw(WithdrawRequest request);

    TransactionResponse getTransactionByReference(String transactionReference);

    List<TransactionResponse> getRecentTransactions(String accountNumber);
}
