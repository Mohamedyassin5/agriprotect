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
public class DecisionDashboardResponse {

    // --- Source des données réelles ---
    private LocalDate analysisPeriodFrom;
    private LocalDate analysisPeriodTo;
    private Long totalIncomeEntriesAnalyzed;
    private Long totalExpenseEntriesAnalyzed;
    private String dataSource;

    private int financialHealthScore;
    private String healthLevel;

    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal monthlySavings;
    private BigDecimal savingsBalance;

    private String profitabilityTrend;
    private double emergencyFundCoverage;

    private List<KeyInsight> keyInsights;
    private List<ActionItem> prioritizedActions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KeyInsight {
        private String icon;
        private String title;
        private String description;
        private String severity;   // INFO, WARNING, ALERT, SUCCESS
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionItem {
        private int priority;
        private String action;
        private String impact;
        private String category;   // SAVINGS, EXPENSES, INCOME, BUDGET
    }
}
