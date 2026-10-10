package com.faiq.navanbillingservice.invoice.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceLineRepository
        extends JpaRepository<InvoiceLineEntity, Long> {

    // Used to check "is this transaction on any invoice?" before PATCH/DELETE
    @Query("SELECT COUNT(l) > 0 FROM InvoiceLineEntity l WHERE l.transactionId = :txnId")
    boolean existsByTransactionId(@Param("txnId") String txnId);
}