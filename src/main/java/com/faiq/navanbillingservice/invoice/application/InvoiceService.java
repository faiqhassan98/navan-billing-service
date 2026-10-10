package com.faiq.navanbillingservice.invoice.application;

import com.faiq.navanbillingservice.common.IdGenerator;
import com.faiq.navanbillingservice.invoice.api.*;
import com.faiq.navanbillingservice.invoice.persistence.*;
import com.faiq.navanbillingservice.transaction.api.PageResponse;
import com.faiq.navanbillingservice.transaction.persistence.TransactionEntity;
import com.faiq.navanbillingservice.transaction.persistence.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvoiceService {

    private static final int MAX_PAGE_SIZE     = 100;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final InvoiceRepository     invoiceRepo;
    private final TransactionRepository transactionRepo;
    private final InvoiceMapper         invoiceMapper;
    private final RollupCalculator      rollupCalculator;

    public InvoiceService(InvoiceRepository invoiceRepo,
                          TransactionRepository transactionRepo,
                          InvoiceMapper invoiceMapper,
                          RollupCalculator rollupCalculator) {
        this.invoiceRepo      = invoiceRepo;
        this.transactionRepo  = transactionRepo;
        this.invoiceMapper    = invoiceMapper;
        this.rollupCalculator = rollupCalculator;
    }

    // ── CREATE ────────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest req) {
        var ids = req.resolvedIds();
        validateNoDuplicateIds(ids);

        var transactions = loadTransactionsOrThrow(ids);
        validateSameCurrency(transactions);

        var invoice = new InvoiceEntity();
        invoice.setId(IdGenerator.invoiceId());
        invoice.setCurrency(transactions.get(0).getCurrency());

        return buildAndSaveInvoice(invoice, transactions);
    }

    // ── GET ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InvoiceResponse getById(String id) {
        return invoiceRepo.findById(id)
                .map(invoiceMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found: " + id));
    }

    // ── LIST ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> list(int page, int size) {
        if (size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "limit must not exceed " + MAX_PAGE_SIZE);
        }
        if (size <= 0) size = DEFAULT_PAGE_SIZE;

        var pageResult = invoiceRepo.findAllPaginated(PageRequest.of(page, size));
        return new PageResponse<>(
                pageResult.getContent().stream()
                        .map(invoiceMapper::toResponse).toList(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getNumber(),
                pageResult.getSize()
        );
    }

    // ── PATCH ─────────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse patch(String id, CreateInvoiceRequest req) {
        var invoice = invoiceRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found: " + id));

        var ids = req.resolvedIds();
        validateNoDuplicateIds(ids);

        var transactions = loadTransactionsOrThrow(ids);
        validateSameCurrency(transactions);

        // Clear existing collections IN PLACE — do not replace the collection
        // reference or Hibernate loses track of orphan deletion
        invoice.getLines().clear();
        invoice.getTaxRollup().clear();
        invoice.getFeeRollup().clear();
        invoice.setCurrency(transactions.get(0).getCurrency());

        // Flush the clears before adding new rows to avoid constraint conflicts
        invoiceRepo.saveAndFlush(invoice);

        return buildAndSaveInvoice(invoice, transactions);
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    @Transactional
    public void delete(String id) {
        var invoice = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found: " + id));
        invoiceRepo.delete(invoice);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────

    /**
     * Builds the invoice snapshot by ADDING to existing collections.
     * Never replaces the collection reference — Hibernate tracks the
     * original collection instance for orphan deletion.
     */
    private InvoiceResponse buildAndSaveInvoice(InvoiceEntity invoice,
                                                 List<TransactionEntity> transactions) {
        // Add snapshot lines to the existing (possibly cleared) collection
        for (var txn : transactions) {
            invoice.getLines().add(invoiceMapper.toSnapshotLine(invoice, txn));
        }

        // Calculate rollups — pure function, no DB access
        var rollup = rollupCalculator.calculate(transactions);

        // Add rollup rows to the existing collections
        rollup.taxRollup().stream()
                .map(r -> new InvoiceTaxRollupEntity(invoice, r.name(), r.rate(), r.amount()))
                .forEach(invoice.getTaxRollup()::add);

        rollup.feeRollup().stream()
                .map(r -> new InvoiceFeeRollupEntity(invoice, r.name(), r.amount()))
                .forEach(invoice.getFeeRollup()::add);

        // Sum totals with BigDecimal — never double
        var grandTotal = transactions.stream()
                .map(TransactionEntity::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setTotal(grandTotal);
        invoice.setTaxTotal(rollup.taxTotal());
        invoice.setFeeTotal(rollup.feeTotal());

        return invoiceMapper.toResponse(invoiceRepo.save(invoice));
    }

    private List<TransactionEntity> loadTransactionsOrThrow(List<String> ids) {
        var transactions = new ArrayList<TransactionEntity>();
        for (var txnId : ids) {
            var txn = transactionRepo.findById(txnId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Transaction not found: " + txnId));
            transactions.add(txn);
        }
        return transactions;
    }

    private void validateSameCurrency(List<TransactionEntity> transactions) {
        var currencies = transactions.stream()
                .map(TransactionEntity::getCurrency)
                .distinct().toList();
        if (currencies.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "All transactions must share the same currency. Found: " + currencies);
        }
    }

    private void validateNoDuplicateIds(List<String> ids) {
        var unique = new java.util.HashSet<>(ids);
        if (unique.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Duplicate transaction ids in request");
        }
    }
}