package com.faiq.navanbillingservice.invoice.application;

import com.faiq.navanbillingservice.common.IdGenerator;
import com.faiq.navanbillingservice.invoice.api.*;
import com.faiq.navanbillingservice.invoice.persistence.*;
import com.faiq.navanbillingservice.transaction.api.PageResponse;
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
        // Lock the invoice row
        var invoice = invoiceRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found: " + id));

        var ids = req.resolvedIds();
        validateNoDuplicateIds(ids);

        var transactions = loadTransactionsOrThrow(ids);
        validateSameCurrency(transactions);

        // Wipe old snapshot — cascade deletes lines, tax/fee rollups
        invoice.getLines().clear();
        invoice.getTaxRollup().clear();
        invoice.getFeeRollup().clear();
        invoice.setCurrency(transactions.get(0).getCurrency());

        return buildAndSaveInvoice(invoice, transactions);
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    @Transactional
    public void delete(String id) {
        var invoice = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found: " + id));
        // Cascade deletes all snapshot lines and rollups
        // Does NOT delete the underlying transactions
        invoiceRepo.delete(invoice);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────

    private InvoiceResponse buildAndSaveInvoice(InvoiceEntity invoice,
                                                 List<com.faiq.navanbillingservice
                                                         .transaction.persistence
                                                         .TransactionEntity> transactions) {
        // Build snapshot lines
        var lines = new ArrayList<InvoiceLineEntity>();
        for (var txn : transactions) {
            lines.add(invoiceMapper.toSnapshotLine(invoice, txn));
        }
        invoice.setLines(lines);

        // Calculate rollups (pure function — no DB)
        var rollup = rollupCalculator.calculate(transactions);

        // Build rollup entities
        var taxRollupEntities = rollup.taxRollup().stream()
                .map(r -> new InvoiceTaxRollupEntity(invoice, r.name(), r.rate(), r.amount()))
                .toList();
        var feeRollupEntities = rollup.feeRollup().stream()
                .map(r -> new InvoiceFeeRollupEntity(invoice, r.name(), r.amount()))
                .toList();
        invoice.setTaxRollup(new ArrayList<>(taxRollupEntities));
        invoice.setFeeRollup(new ArrayList<>(feeRollupEntities));

        // Calculate totals
        var grandTotal = transactions.stream()
                .map(t -> t.getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setTotal(grandTotal);
        invoice.setTaxTotal(rollup.taxTotal());
        invoice.setFeeTotal(rollup.feeTotal());

        var saved = invoiceRepo.save(invoice);
        return invoiceMapper.toResponse(saved);
    }

    private List<com.faiq.navanbillingservice.transaction.persistence.TransactionEntity>
            loadTransactionsOrThrow(List<String> ids) {
        var transactions = new ArrayList<com.faiq.navanbillingservice
                .transaction.persistence.TransactionEntity>();
        for (var txnId : ids) {
            var txn = transactionRepo.findById(txnId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Transaction not found: " + txnId));
            transactions.add(txn);
        }
        return transactions;
    }

    private void validateSameCurrency(
            List<com.faiq.navanbillingservice.transaction.persistence
                    .TransactionEntity> transactions) {
        var currencies = transactions.stream()
                .map(t -> t.getCurrency())
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