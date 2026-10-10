package com.faiq.navanbillingservice.transaction.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionTaxLineRepository
        extends JpaRepository<TransactionTaxLineEntity, Long> {

    @Modifying
    @Query("DELETE FROM TransactionTaxLineEntity t WHERE t.transaction.id = :txnId")
    void deleteByTransactionId(@Param("txnId") String txnId);
}