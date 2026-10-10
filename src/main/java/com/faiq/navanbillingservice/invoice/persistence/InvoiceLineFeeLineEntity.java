package com.faiq.navanbillingservice.invoice.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_line_fee_lines")
public class InvoiceLineFeeLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_line_id", nullable = false)
    private InvoiceLineEntity invoiceLine;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private int position;

    public InvoiceLineFeeLineEntity() {}

    public InvoiceLineFeeLineEntity(InvoiceLineEntity invoiceLine,
                                    String name,
                                    BigDecimal amount,
                                    int position) {
        this.invoiceLine = invoiceLine;
        this.name        = name;
        this.amount      = amount;
        this.position    = position;
    }

    public Long getId()                       { return id; }
    public InvoiceLineEntity getInvoiceLine() { return invoiceLine; }
    public String getName()                   { return name; }
    public BigDecimal getAmount()             { return amount; }
    public int getPosition()                  { return position; }
}