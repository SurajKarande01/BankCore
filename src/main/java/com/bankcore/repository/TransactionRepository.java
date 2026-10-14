package com.bankcore.repository;

import com.bankcore.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {

    Optional<TransactionRecord> findByTransactionReference(String transactionReference);

    /**
     * Finds all transactions involving the given account within a specific date range,
     * ordered chronologically for monthly statement generation.
     */
    @Query("SELECT t FROM TransactionRecord t WHERE (t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId) " +
           "AND t.createdAt >= :startDate AND t.createdAt <= :endDate ORDER BY t.createdAt ASC")
    List<TransactionRecord> findStatementTransactions(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Finds recent transactions for an account ordered newest to oldest.
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId " +
           "ORDER BY t.createdAt DESC")
    List<TransactionRecord> findRecentTransactions(@Param("accountId") Long accountId);
}
