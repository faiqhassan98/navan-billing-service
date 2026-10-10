package com.faiq.navanbillingservice.transaction.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionFeeLineRepository
        extends JpaRepository<TransactionFeeLineEntity, Long> {

    @Modifying
    @Query("DELETE FROM TransactionFeeLineEntity f WHERE f.transaction.id = :txnId")
    void deleteByTransactionId(@Param("txnId") String txnId);
}