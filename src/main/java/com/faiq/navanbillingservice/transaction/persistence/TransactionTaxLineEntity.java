package com.faiq.navanbillingservice.transaction.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "transaction_tax_lines")
public class TransactionTaxLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private TransactionEntity transaction;

    @Column(nullable = false)
    private String name;

    @Column(precision = 8, scale = 4)
    private BigDecimal rate;           // nullable — Airport tax has no rate

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private int position;

    public TransactionTaxLineEntity() {}

    public TransactionTaxLineEntity(TransactionEntity transaction,
                                    String name,
                                    BigDecimal rate,
                                    BigDecimal amount,
                                    int position) {
        this.transaction = transaction;
        this.name        = name;
        this.rate        = rate;
        this.amount      = amount;
        this.position    = position;
    }

    public Long getId()                  { return id; }
    public TransactionEntity getTransaction() { return transaction; }
    public String getName()              { return name; }
    public BigDecimal getRate()          { return rate; }
    public BigDecimal getAmount()        { return amount; }
    public int getPosition()             { return position; }

    public void setName(String name)          { this.name = name; }
    public void setRate(BigDecimal rate)      { this.rate = rate; }
    public void setAmount(BigDecimal amount)  { this.amount = amount; }
    public void setPosition(int position)     { this.position = position; }
}