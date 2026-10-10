package com.faiq.navanbillingservice.transaction.persistence;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "external_id", nullable = false, unique = true, length = 255)
    private String externalId;

    @Column(nullable = false, length = 20)
    private String type;   // stored as lowercase: flight, hotel, rail, navan_fee

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "transaction",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderColumn(name = "position")
    private List<TransactionTaxLineEntity> taxLines = new ArrayList<>();

    @OneToMany(mappedBy = "transaction",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderColumn(name = "position")
    private List<TransactionFeeLineEntity> feeLines = new ArrayList<>();

    public TransactionEntity() {}

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
    public String getId()                               { return id; }
    public String getExternalId()                       { return externalId; }
    public String getType()                             { return type; }
    public String getCurrency()                         { return currency; }
    public Instant getOccurredAt()                      { return occurredAt; }
    public BigDecimal getTotal()                        { return total; }
    public Map<String, Object> getMetadata()            { return metadata; }
    public Long getVersion()                            { return version; }
    public Instant getCreatedAt()                       { return createdAt; }
    public Instant getUpdatedAt()                       { return updatedAt; }
    public List<TransactionTaxLineEntity> getTaxLines() { return taxLines; }
    public List<TransactionFeeLineEntity> getFeeLines() { return feeLines; }

    // ── Setters ──────────────────────────────────────────────────────────
    public void setId(String id)                              { this.id = id; }
    public void setExternalId(String externalId)              { this.externalId = externalId; }
    public void setType(String type)                          { this.type = type; }
    public void setCurrency(String currency)                  { this.currency = currency; }
    public void setOccurredAt(Instant occurredAt)             { this.occurredAt = occurredAt; }
    public void setTotal(BigDecimal total)                    { this.total = total; }
    public void setMetadata(Map<String, Object> metadata)     { this.metadata = metadata; }
    public void setTaxLines(List<TransactionTaxLineEntity> l) { this.taxLines = l; }
    public void setFeeLines(List<TransactionFeeLineEntity> l) { this.feeLines = l; }
}