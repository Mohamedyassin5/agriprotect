package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSavingsPlanResponse {

    // --- Source des données réelles (accounting_entry sur 12 mois) ---
    private LocalDate historicalPeriodFrom;         // Début de la période d'analyse (il y a 12 mois)
    private LocalDate historicalPeriodTo;           // Fin de la période d'analyse (aujourd'hui)
    private int monthsWithAccountingData;           // Mois avec des données réelles (revenus ou dépenses)
    private BigDecimal averageMonthlyDisposableIncome; // Revenu net mensuel moyen réel (revenus - dépenses)
    private String dataSource;                      // Explication de la méthode utilisée

    // --- Objectif et situation actuelle ---
    private BigDecimal goalAmount;                  // Somme totale des targets de vos goals actifs
    private BigDecimal currentBalance;              // Solde actuel du compte épargne
    private BigDecimal remaining;                   // Montant restant à atteindre
    private BigDecimal recommendedMonthlySavings;   // Montant recommandé par mois (lissé sur la durée)
    private int planDurationMonths;
    private List<MonthlyPlan> monthlyPlan;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyPlan {
        private String month;
        private BigDecimal recommendedSavings;
        private BigDecimal projectedBalance;
        private double progressPercentage;
        private String seasonalNote;
    }
}
