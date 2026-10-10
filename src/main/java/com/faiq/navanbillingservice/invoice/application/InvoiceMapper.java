package com.faiq.navanbillingservice.invoice.application;

import com.faiq.navanbillingservice.invoice.api.*;
import com.faiq.navanbillingservice.invoice.persistence.*;
import com.faiq.navanbillingservice.transaction.api.FeeLineResponse;
import com.faiq.navanbillingservice.transaction.api.TaxLineResponse;
import com.faiq.navanbillingservice.transaction.persistence.TransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    public InvoiceResponse toResponse(InvoiceEntity e) {
        return new InvoiceResponse(
                e.getId(),
                e.getCurrency(),
                e.getTotal(),
                e.getTaxTotal(),
                e.getFeeTotal(),
                e.getLines().stream().map(this::toLineResponse).toList(),
                e.getTaxRollup().stream()
                        .map(r -> new TaxRollupResponse(r.getName(), r.getRate(), r.getAmount()))
                        .toList(),
                e.getFeeRollup().stream()
                        .map(r -> new FeeRollupResponse(r.getName(), r.getAmount()))
                        .toList(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    private InvoiceLineResponse toLineResponse(InvoiceLineEntity l) {
        return new InvoiceLineResponse(
                l.getTransactionId(),
                l.getExternalId(),
                l.getType(),
                l.getCurrency(),
                l.getOccurredAt(),
                l.getTotal(),
                l.getTaxLines().stream()
                        .map(t -> new TaxLineResponse(t.getName(), t.getRate(), t.getAmount()))
                        .toList(),
                l.getFeeLines().stream()
                        .map(f -> new FeeLineResponse(f.getName(), f.getAmount()))
                        .toList(),
                l.getMetadata()
        );
    }

    /**
     * Builds a frozen snapshot InvoiceLine from a live transaction.
     * This copy is independent — later edits to the transaction won't affect it.
     */
    public InvoiceLineEntity toSnapshotLine(InvoiceEntity invoice,
                                            TransactionEntity txn) {
        var line = new InvoiceLineEntity();
        line.setInvoice(invoice);
        line.setTransactionId(txn.getId());
        line.setExternalId(txn.getExternalId());
        line.setType(txn.getType());
        line.setCurrency(txn.getCurrency());
        line.setOccurredAt(txn.getOccurredAt());
        line.setTotal(txn.getTotal());
        line.setMetadata(txn.getMetadata());

        // Snapshot tax lines
        var taxLines = new java.util.ArrayList<InvoiceLineTaxLineEntity>();
        var txnTaxLines = txn.getTaxLines();
        for (int i = 0; i < txnTaxLines.size(); i++) {
            var t = txnTaxLines.get(i);
            taxLines.add(new InvoiceLineTaxLineEntity(
                    line, t.getName(), t.getRate(), t.getAmount(), i));
        }
        line.setTaxLines(taxLines);

        // Snapshot fee lines
        var feeLines = new java.util.ArrayList<InvoiceLineFeeLineEntity>();
        var txnFeeLines = txn.getFeeLines();
        for (int i = 0; i < txnFeeLines.size(); i++) {
            var f = txnFeeLines.get(i);
            feeLines.add(new InvoiceLineFeeLineEntity(
                    line, f.getName(), f.getAmount(), i));
        }
        line.setFeeLines(feeLines);

        return line;
    }
}