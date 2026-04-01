package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.agri.entities.enums.EntryCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatIfSimulationResponse {

    // --- Source des données réelles ---
    private LocalDate analysisPeriodFrom;           // Début de la période analysée (il y a 3 mois)
    private LocalDate analysisPeriodTo;             // Fin de la période analysée (aujourd'hui)
    private Long totalIncomeEntriesAnalyzed;        // Nombre d'écritures INCOME réelles utilisées
    private Long totalExpenseEntriesAnalyzed;       // Nombre d'écritures EXPENSE réelles utilisées
    private String dataSource;                      // Explication lisible de la période et méthode

    // --- Situation actuelle (moyenne mensuelle réelle sur 3 mois) ---
    private BigDecimal currentMonthlyIncome;
    private BigDecimal currentMonthlyExpenses;
    private BigDecimal currentNetIncome;

    // --- Situation simulée ---
    private BigDecimal simulatedMonthlyIncome;
    private BigDecimal simulatedMonthlyExpenses;
    private BigDecimal simulatedNetIncome;

    private BigDecimal netImpact;
    private String verdict;   // POSITIVE, NEGATIVE, NEUTRAL

    private List<String> feasibilityWarnings;  // Avertissements sur la faisabilité des changements demandés

    private Map<EntryCategory, CategoryImpact> categoryImpacts;
    private List<MonthProjection> monthlyProjections;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryImpact {
        private BigDecimal realTotalLast3Months;    // Somme brute réelle des 3 derniers mois (depuis accounting_entry)
        private int numberOfEntries;                // Nombre d'écritures réelles dans cette catégorie sur la période
        private BigDecimal currentAmount;           // Moyenne mensuelle réelle (realTotal / 3 mois)
        private BigDecimal simulatedAmount;         // currentAmount × (1 + changePercent/100)
        private double changePercent;               // % appliqué par l'utilisateur (ex: -20 = réduction de 20%)
        private String formula;                     // Explication lisible du calcul appliqué
        private String feasibility;                 // REALISTIC / AGGRESSIVE / UNREALISTIC / NO_DATA
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthProjection {
        private String month;
        private BigDecimal projectedIncome;
        private BigDecimal projectedExpenses;
        private BigDecimal projectedNet;
        private BigDecimal cumulativeSavings;
    }
}
