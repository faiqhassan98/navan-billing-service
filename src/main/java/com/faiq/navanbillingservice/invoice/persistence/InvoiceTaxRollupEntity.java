package com.faiq.navanbillingservice.invoice.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_tax_rollup")
public class InvoiceTaxRollupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(nullable = false)
    private String name;

    @Column(precision = 8, scale = 4)
    private BigDecimal rate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    public InvoiceTaxRollupEntity() {}

    public InvoiceTaxRollupEntity(InvoiceEntity invoice,
                                  String name,
                                  BigDecimal rate,
                                  BigDecimal amount) {
        this.invoice = invoice;
        this.name    = name;
        this.rate    = rate;
        this.amount  = amount;
    }

    public Long getId()              { return id; }
    public InvoiceEntity getInvoice(){ return invoice; }
    public String getName()          { return name; }
    public BigDecimal getRate()      { return rate; }
    public BigDecimal getAmount()    { return amount; }
}