package com.faiq.navanbillingservice.invoice.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_fee_rollup")
public class InvoiceFeeRollupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    public InvoiceFeeRollupEntity() {}

    public InvoiceFeeRollupEntity(InvoiceEntity invoice,
                                  String name,
                                  BigDecimal amount) {
        this.invoice = invoice;
        this.name    = name;
        this.amount  = amount;
    }

    public Long getId()               { return id; }
    public InvoiceEntity getInvoice() { return invoice; }
    public String getName()           { return name; }
    public BigDecimal getAmount()     { return amount; }
}