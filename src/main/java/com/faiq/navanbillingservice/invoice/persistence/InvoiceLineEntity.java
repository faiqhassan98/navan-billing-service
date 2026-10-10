package com.faiq.navanbillingservice.invoice.persistence;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "invoice_lines")
public class InvoiceLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    /**
     * Plain column — no FK to transactions table.
     * This is a snapshot: the invoice line is independent of the live transaction.
     * However, Postgres RESTRICT on the FK in V1__init.sql blocks
     * deleting a transaction that is referenced here.
     */
    @Column(name = "transaction_id", nullable = false, length = 36)
    private String transactionId;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata;

    @OneToMany(mappedBy = "invoiceLine",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderColumn(name = "position")
    private List<InvoiceLineTaxLineEntity> taxLines = new ArrayList<>();

    @OneToMany(mappedBy = "invoiceLine",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderColumn(name = "position")
    private List<InvoiceLineFeeLineEntity> feeLines = new ArrayList<>();

    public InvoiceLineEntity() {}

    // ── Getters ──────────────────────────────────────────────────────────
    public Long getId()                                  { return id; }
    public InvoiceEntity getInvoice()                    { return invoice; }
    public String getTransactionId()                     { return transactionId; }
    public String getExternalId()                        { return externalId; }
    public String getType()                              { return type; }
    public String getCurrency()                          { return currency; }
    public Instant getOccurredAt()                       { return occurredAt; }
    public BigDecimal getTotal()                         { return total; }
    public Map<String, Object> getMetadata()             { return metadata; }
    public List<InvoiceLineTaxLineEntity> getTaxLines()  { return taxLines; }
    public List<InvoiceLineFeeLineEntity> getFeeLines()  { return feeLines; }

    // ── Setters ──────────────────────────────────────────────────────────
    public void setInvoice(InvoiceEntity invoice)               { this.invoice = invoice; }
    public void setTransactionId(String transactionId)          { this.transactionId = transactionId; }
    public void setExternalId(String externalId)                { this.externalId = externalId; }
    public void setType(String type)                            { this.type = type; }
    public void setCurrency(String currency)                    { this.currency = currency; }
    public void setOccurredAt(Instant occurredAt)               { this.occurredAt = occurredAt; }
    public void setTotal(BigDecimal total)                      { this.total = total; }
    public void setMetadata(Map<String, Object> metadata)       { this.metadata = metadata; }
    public void setTaxLines(List<InvoiceLineTaxLineEntity> l)   { this.taxLines = l; }
    public void setFeeLines(List<InvoiceLineFeeLineEntity> l)   { this.feeLines = l; }
}