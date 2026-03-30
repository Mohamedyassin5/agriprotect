package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.agri.entities.enums.EntryCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashFlowOptimizerResponse {

    // --- Source des données réelles ---
    private LocalDate analysisPeriodFrom;
    private LocalDate analysisPeriodTo;
    private String dataSource;

    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal currentMonthlyNet;
    private BigDecimal optimizedMonthlyNet;
    private BigDecimal potentialImprovement;

    private List<OptimizationSuggestion> suggestions;
    private BigDecimal idealSavingsAllocation;
    private BigDecimal idealExpensesCeiling;
    private String overallStrategy;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptimizationSuggestion {
        private int priority;
        private EntryCategory category;
        private String type;                    // REDUCE_EXPENSE, INCREASE_INCOME, REALLOCATE
        private BigDecimal realTotalLast3Months; // Somme brute réelle des 3 derniers mois
        private int numberOfEntries;             // Nombre d'écritures dans cette catégorie sur la période
        private BigDecimal currentAmount;        // Moyenne mensuelle réelle (realTotal / 3)
        private BigDecimal suggestedAmount;
        private BigDecimal potentialSaving;
        private String justification;
    }
}
