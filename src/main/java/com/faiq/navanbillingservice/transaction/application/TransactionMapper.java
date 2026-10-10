package com.faiq.navanbillingservice.transaction.application;

import com.faiq.navanbillingservice.transaction.api.*;
import com.faiq.navanbillingservice.transaction.domain.TransactionType;
import com.faiq.navanbillingservice.transaction.persistence.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(TransactionEntity e) {
        return new TransactionResponse(
                e.getId(),
                e.getExternalId(),
                TransactionType.fromValue(e.getType()),
                e.getCurrency(),
                e.getOccurredAt(),
                e.getTotal(),
                e.getTaxLines().stream()
                        .map(t -> new TaxLineResponse(t.getName(), t.getRate(), t.getAmount()))
                        .toList(),
                e.getFeeLines().stream()
                        .map(f -> new FeeLineResponse(f.getName(), f.getAmount()))
                        .toList(),
                e.getMetadata(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public List<TransactionTaxLineEntity> toTaxLineEntities(
            TransactionEntity txn,
            List<TaxLineRequest> requests) {
        var result = new java.util.ArrayList<TransactionTaxLineEntity>();
        for (int i = 0; i < requests.size(); i++) {
            var r = requests.get(i);
            result.add(new TransactionTaxLineEntity(txn, r.name(), r.rate(), r.amount(), i));
        }
        return result;
    }

    public List<TransactionFeeLineEntity> toFeeLineEntities(
            TransactionEntity txn,
            List<FeeLineRequest> requests) {
        var result = new java.util.ArrayList<TransactionFeeLineEntity>();
        for (int i = 0; i < requests.size(); i++) {
            var r = requests.get(i);
            result.add(new TransactionFeeLineEntity(txn, r.name(), r.amount(), i));
        }
        return result;
    }
}