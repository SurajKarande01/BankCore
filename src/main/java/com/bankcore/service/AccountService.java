package com.bankcore.service;

import com.bankcore.dto.request.CreateAccountRequest;
import com.bankcore.dto.response.AccountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccountByNumber(String accountNumber);

    AccountResponse getAccountById(Long id);

    Page<AccountResponse> getAllAccounts(Pageable pageable);
}
