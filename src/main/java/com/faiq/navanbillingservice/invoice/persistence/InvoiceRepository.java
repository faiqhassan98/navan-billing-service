package com.faiq.navanbillingservice.invoice.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceRepository
        extends JpaRepository<InvoiceEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InvoiceEntity i WHERE i.id = :id")
    Optional<InvoiceEntity> findByIdForUpdate(@Param("id") String id);

    @Query("SELECT i FROM InvoiceEntity i ORDER BY i.createdAt ASC, i.id ASC")
    Page<InvoiceEntity> findAllPaginated(Pageable pageable);
}