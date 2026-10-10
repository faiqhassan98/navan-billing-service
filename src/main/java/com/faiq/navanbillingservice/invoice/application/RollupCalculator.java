package com.faiq.navanbillingservice.invoice.application;

import com.faiq.navanbillingservice.invoice.api.FeeRollupResponse;
import com.faiq.navanbillingservice.invoice.api.TaxRollupResponse;
import com.faiq.navanbillingservice.transaction.persistence.TransactionEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * Pure calculation — no database access.
 * Groups tax lines by (name, rate) and fee lines by name,
 * then sums amounts with BigDecimal precision.
 * Keeping this separate makes it trivially unit-testable.
 */
@Component
public class RollupCalculator {

    public record RollupResult(
            List<TaxRollupResponse> taxRollup,
            List<FeeRollupResponse> feeRollup,
            BigDecimal taxTotal,
            BigDecimal feeTotal
    ) {}

    public RollupResult calculate(List<TransactionEntity> transactions) {

        // Tax rollup: group by (name, rate)
        record TaxKey(String name, BigDecimal rate) {}
        Map<TaxKey, BigDecimal> taxMap = new LinkedHashMap<>();

        for (var txn : transactions) {
            for (var tax : txn.getTaxLines()) {
                var key = new TaxKey(tax.getName(), tax.getRate());
                taxMap.merge(key, tax.getAmount(), BigDecimal::add);
            }
        }

        // Fee rollup: group by name
        Map<String, BigDecimal> feeMap = new LinkedHashMap<>();
        for (var txn : transactions) {
            for (var fee : txn.getFeeLines()) {
                feeMap.merge(fee.getName(), fee.getAmount(), BigDecimal::add);
            }
        }

        var taxRollup = taxMap.entrySet().stream()
                .map(e -> new TaxRollupResponse(e.getKey().name(),
                                                e.getKey().rate(),
                                                e.getValue()))
                .toList();

        var feeRollup = feeMap.entrySet().stream()
                .map(e -> new FeeRollupResponse(e.getKey(), e.getValue()))
                .toList();

        var taxTotal = taxRollup.stream()
                .map(TaxRollupResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var feeTotal = feeRollup.stream()
                .map(FeeRollupResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RollupResult(taxRollup, feeRollup, taxTotal, feeTotal);
    }
}