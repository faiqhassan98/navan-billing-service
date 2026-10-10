package com.faiq.navanbillingservice.transaction.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<TransactionEntity, String> {

    Optional<TransactionEntity> findByExternalId(String externalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TransactionEntity t WHERE t.id = :id")
    Optional<TransactionEntity> findByIdForUpdate(@Param("id") String id);

    @Query("SELECT t FROM TransactionEntity t ORDER BY t.createdAt ASC, t.id ASC")
    Page<TransactionEntity> findAllPaginated(Pageable pageable);
}