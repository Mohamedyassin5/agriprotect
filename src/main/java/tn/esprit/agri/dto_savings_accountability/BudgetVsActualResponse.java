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
public class BudgetVsActualResponse {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryComparison {
        private EntryCategory category;
        private BigDecimal budgetedAmount;
        private BigDecimal actualAmount;
        private BigDecimal variance;
        private Double percentageUsed;
        private String status;
    }

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private List<CategoryComparison> comparisons;
    private BigDecimal totalBudgeted;
    private BigDecimal totalActual;
    private BigDecimal totalVariance;
}
