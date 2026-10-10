package com.faiq.navanbillingservice.transaction.application;

import com.faiq.navanbillingservice.common.IdGenerator;
import com.faiq.navanbillingservice.transaction.api.*;
import com.faiq.navanbillingservice.transaction.domain.TransactionType;
import com.faiq.navanbillingservice.transaction.persistence.*;
import com.faiq.navanbillingservice.invoice.persistence.InvoiceLineRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransactionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final TransactionRepository       transactionRepo;
    private final TransactionTaxLineRepository taxLineRepo;
    private final TransactionFeeLineRepository feeLineRepo;
    private final InvoiceLineRepository       invoiceLineRepo;
    private final MetadataMapper              metadataMapper;
    private final TransactionMapper           transactionMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public TransactionService(
            TransactionRepository transactionRepo,
            TransactionTaxLineRepository taxLineRepo,
            TransactionFeeLineRepository feeLineRepo,
            InvoiceLineRepository invoiceLineRepo,
            MetadataMapper metadataMapper,
            TransactionMapper transactionMapper,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.transactionRepo   = transactionRepo;
        this.taxLineRepo       = taxLineRepo;
        this.feeLineRepo       = feeLineRepo;
        this.invoiceLineRepo   = invoiceLineRepo;
        this.metadataMapper    = metadataMapper;
        this.transactionMapper = transactionMapper;
        this.objectMapper      = objectMapper;
    }

    // ── CREATE ────────────────────────────────────────────────────────────

    public TransactionResponse create(CreateTransactionRequest req) {
        // Parse and validate type
        TransactionType type;
        try {
            type = TransactionType.fromValue(req.type());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        // Parse and validate metadata
        var typedMetadata = metadataMapper.parse(type, req.metadata());
        var metadataMap   = metadataMapper.toMap(typedMetadata);

        try {
            return doCreate(req, type, metadataMap);
        } catch (DataIntegrityViolationException e) {
            // Unique constraint on external_id fired — return existing record
            TransactionEntity existing = transactionRepo.findByExternalId(req.externalId())
                    .orElseThrow(() -> e);
            throw new DuplicateTransactionException(existing.getId());
        }
    }

    @Transactional
    protected TransactionResponse doCreate(
            CreateTransactionRequest req,
            TransactionType type,
            java.util.Map<String, Object> metadataMap) {

        var entity = new TransactionEntity();
        entity.setId(IdGenerator.transactionId());
        entity.setExternalId(req.externalId());
        entity.setType(type.getValue());
        entity.setCurrency(req.currency().toUpperCase());
        entity.setOccurredAt(req.occurredAt());
        entity.setTotal(req.total());
        entity.setMetadata(metadataMap);

        // Save header first so child entities have a parent
        var saved = transactionRepo.saveAndFlush(entity);

        // Save tax and fee lines
        var taxLines = transactionMapper.toTaxLineEntities(saved, req.taxLines());
        var feeLines = transactionMapper.toFeeLineEntities(saved, req.feeLines());
        taxLineRepo.saveAll(taxLines);
        feeLineRepo.saveAll(feeLines);
        saved.setTaxLines(taxLines);
        saved.setFeeLines(feeLines);

        return transactionMapper.toResponse(saved);
    }

    // ── GET ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TransactionResponse getById(String id) {
        return transactionRepo.findById(id)
                .map(transactionMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transaction not found: " + id));
    }

    // ── LIST ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> list(int page, int size) {
        if (size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "limit must not exceed " + MAX_PAGE_SIZE);
        }
        if (size <= 0) size = DEFAULT_PAGE_SIZE;

        var pageResult = transactionRepo.findAllPaginated(PageRequest.of(page, size));
        return new PageResponse<>(
                pageResult.getContent().stream()
                        .map(transactionMapper::toResponse).toList(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getNumber(),
                pageResult.getSize()
        );
    }

    // ── PATCH ─────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse patch(String id, PatchTransactionRequest req) {
        var entity = transactionRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transaction not found: " + id));

        // Block patch if transaction is on any invoice
        if (invoiceLineRepo.existsByTransactionId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transaction " + id + " is already on an invoice and cannot be modified");
        }

        if (req.total()      != null) entity.setTotal(req.total());
        if (req.occurredAt() != null) entity.setOccurredAt(req.occurredAt());

        if (req.metadata() != null) {
            var type          = TransactionType.fromValue(entity.getType());
            var typedMetadata = metadataMapper.parse(type, req.metadata());
            entity.setMetadata(metadataMapper.toMap(typedMetadata));
        }

        if (req.taxLines() != null) {
            taxLineRepo.deleteByTransactionId(id);
            var newLines = transactionMapper.toTaxLineEntities(entity, req.taxLines());
            taxLineRepo.saveAll(newLines);
            entity.setTaxLines(newLines);
        }

        if (req.feeLines() != null) {
            feeLineRepo.deleteByTransactionId(id);
            var newLines = transactionMapper.toFeeLineEntities(entity, req.feeLines());
            feeLineRepo.saveAll(newLines);
            entity.setFeeLines(newLines);
        }

        return transactionMapper.toResponse(transactionRepo.save(entity));
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    @Transactional
    public void delete(String id) {
        var entity = transactionRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transaction not found: " + id));

        // Postgres RESTRICT on invoice_lines FK also blocks this at DB level
        if (invoiceLineRepo.existsByTransactionId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transaction " + id + " is on an invoice and cannot be deleted");
        }

        transactionRepo.delete(entity);
    }
}