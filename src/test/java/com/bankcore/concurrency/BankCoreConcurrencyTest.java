package com.bankcore.concurrency;

import com.bankcore.dto.request.CreateAccountRequest;
import com.bankcore.dto.request.FundTransferRequest;
import com.bankcore.dto.response.AccountResponse;
import com.bankcore.entity.enums.AccountType;
import com.bankcore.service.AccountService;
import com.bankcore.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BankCoreConcurrencyTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Test
    @DisplayName("Simulate 10 concurrent transfers from Account A to Account B to prove pessimistic locking prevents lost updates")
    void testConcurrentTransfersWithoutLostUpdates() throws InterruptedException {
        // Create Sender Account with 1000.00
        AccountResponse sender = accountService.createAccount(CreateAccountRequest.builder()
                .accountHolderName("Sender Stress Test")
                .email("sender_stress@bankcore.com")
                .phoneNumber("+919999000001")
                .accountType(AccountType.SAVINGS)
                .initialDeposit(new BigDecimal("1000.00"))
                .createdBy("stress_test")
                .build());

        // Create Receiver Account with 0.00
        AccountResponse receiver = accountService.createAccount(CreateAccountRequest.builder()
                .accountHolderName("Receiver Stress Test")
                .email("receiver_stress@bankcore.com")
                .phoneNumber("+919999000002")
                .accountType(AccountType.SAVINGS)
                .initialDeposit(BigDecimal.ZERO)
                .createdBy("stress_test")
                .build());

        int numberOfThreads = 10;
        BigDecimal transferAmount = new BigDecimal("50.00"); // 10 * 50 = 500 total transferred

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successfulTransfers = new AtomicInteger(0);
        AtomicInteger failedTransfers = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    // Wait until all threads are queued up and release simultaneously
                    startLatch.await();

                    transactionService.transferFunds(FundTransferRequest.builder()
                            .sourceAccountNumber(sender.getAccountNumber())
                            .destinationAccountNumber(receiver.getAccountNumber())
                            .amount(transferAmount)
                            .description("Concurrent Transfer Thread #" + threadIndex)
                            .initiatedBy("worker_thread_" + threadIndex)
                            .build());

                    successfulTransfers.incrementAndGet();
                } catch (Exception e) {
                    failedTransfers.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all 10 threads at the exact same instant
        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent transfer threads must complete within timeout");
        assertEquals(10, successfulTransfers.get(), "All 10 transfers should succeed with sufficient balance");
        assertEquals(0, failedTransfers.get(), "No transfers should fail or throw unexpected exceptions");

        // Verify mathematical balances after concurrency
        AccountResponse updatedSender = accountService.getAccountByNumber(sender.getAccountNumber());
        AccountResponse updatedReceiver = accountService.getAccountByNumber(receiver.getAccountNumber());

        // Sender started with 1000, 10 * 50 = 500 transferred => 500 remaining
        assertEquals(new BigDecimal("500.00"), updatedSender.getBalance(),
                "Sender balance must exactly equal 500.00 with zero lost updates");

        // Receiver started with 0, received 500 => 500 total
        assertEquals(new BigDecimal("500.00"), updatedReceiver.getBalance(),
                "Receiver balance must exactly equal 500.00 with zero lost updates");
    }
}
