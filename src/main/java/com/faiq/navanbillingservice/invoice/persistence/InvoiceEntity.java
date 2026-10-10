package com.faiq.navanbillingservice.invoice.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
public class InvoiceEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    @Column(name = "tax_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxTotal;

    @Column(name = "fee_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeTotal;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "invoice",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<InvoiceLineEntity> lines = new ArrayList<>();

    @OneToMany(mappedBy = "invoice",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<InvoiceTaxRollupEntity> taxRollup = new ArrayList<>();

    @OneToMany(mappedBy = "invoice",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<InvoiceFeeRollupEntity> feeRollup = new ArrayList<>();

    public InvoiceEntity() {}

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // ── Getters ──────────────────────────────────────────────────────────
    public String getId()                                { return id; }
    public String getCurrency()                          { return currency; }
    public BigDecimal getTotal()                         { return total; }
    public BigDecimal getTaxTotal()                      { return taxTotal; }
    public BigDecimal getFeeTotal()                      { return feeTotal; }
    public Long getVersion()                             { return version; }
    public Instant getCreatedAt()                        { return createdAt; }
    public Instant getUpdatedAt()                        { return updatedAt; }
    public List<InvoiceLineEntity> getLines()            { return lines; }
    public List<InvoiceTaxRollupEntity> getTaxRollup()  { return taxRollup; }
    public List<InvoiceFeeRollupEntity> getFeeRollup()  { return feeRollup; }

    // ── Setters ──────────────────────────────────────────────────────────
    public void setId(String id)                                    { this.id = id; }
    public void setCurrency(String currency)                        { this.currency = currency; }
    public void setTotal(BigDecimal total)                          { this.total = total; }
    public void setTaxTotal(BigDecimal taxTotal)                    { this.taxTotal = taxTotal; }
    public void setFeeTotal(BigDecimal feeTotal)                    { this.feeTotal = feeTotal; }
    public void setLines(List<InvoiceLineEntity> lines)             { this.lines = lines; }
    public void setTaxRollup(List<InvoiceTaxRollupEntity> rollup)  { this.taxRollup = rollup; }
    public void setFeeRollup(List<InvoiceFeeRollupEntity> rollup)  { this.feeRollup = rollup; }
}