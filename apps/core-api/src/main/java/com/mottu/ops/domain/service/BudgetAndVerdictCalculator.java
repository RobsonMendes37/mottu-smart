package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.ReportedOriginContext;
import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.domain.model.DamageItem;
import com.mottu.ops.domain.model.Money;
import com.mottu.ops.domain.model.RepairBudget;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class BudgetAndVerdictCalculator {

    public static final Money DEFAULT_STANDARD_LABOR_FEE = Money.of(BigDecimal.valueOf(35.00));

    /**
     * Determines suggested damage verdict based on rider context, 48h/100km eligibility, and part nature.
     */
    public DamageVerdict determineVerdict(
            ReportedOriginContext context,
            boolean isPreExistingEligible,
            boolean allPartsNaturalWear
    ) {
        Objects.requireNonNull(context, "ReportedOriginContext cannot be null");

        if (context == ReportedOriginContext.RECEIVED_LIKE_THIS && isPreExistingEligible) {
            return DamageVerdict.PRE_EXISTENTE;
        }

        if (context == ReportedOriginContext.NORMAL_USE_WEAR && allPartsNaturalWear) {
            return DamageVerdict.DESGASTE_NATURAL;
        }

        return DamageVerdict.MAU_USO;
    }

    /**
     * Calculates repair budget using default standard labor fee (R$ 35,00).
     */
    public RepairBudget calculateBudget(DamageVerdict verdict, List<DamageItem> items) {
        return calculateBudget(verdict, items, DEFAULT_STANDARD_LABOR_FEE);
    }

    /**
     * Calculates repair budget with customizable labor fee (e.g. from Manager settings).
     */
    public RepairBudget calculateBudget(DamageVerdict verdict, List<DamageItem> items, Money laborFee) {
        Objects.requireNonNull(verdict, "DamageVerdict cannot be null");
        Money applicableLaborFee = laborFee != null ? laborFee : DEFAULT_STANDARD_LABOR_FEE;

        if (items != null) {
            for (DamageItem item : items) {
                if (item.unitPrice().getAmount().compareTo(BigDecimal.ZERO) < 0) {
                    throw new DomainValidationException("Damage item price cannot be negative: " + item.partName());
                }
            }
        }

        Money itemsSum = Money.ZERO;
        if (items != null && !items.isEmpty()) {
            for (DamageItem item : items) {
                itemsSum = itemsSum.add(item.unitPrice());
            }
        }

        if (verdict == DamageVerdict.PRE_EXISTENTE || verdict == DamageVerdict.DESGASTE_NATURAL) {
            Money absorbed = itemsSum.add(applicableLaborFee);
            return new RepairBudget(
                    verdict,
                    Money.ZERO,
                    Money.ZERO,
                    Money.ZERO,
                    absorbed
            );
        }

        // MAU_USO
        if (items == null || items.isEmpty()) {
            return RepairBudget.zero(DamageVerdict.MAU_USO);
        }

        Money total = itemsSum.add(applicableLaborFee);
        return new RepairBudget(
                DamageVerdict.MAU_USO,
                itemsSum,
                applicableLaborFee,
                total,
                Money.ZERO
        );
    }
}
