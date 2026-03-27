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
public class SpendingBreakdownResponse {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryBreakdown {
        private EntryCategory category;
        private BigDecimal amount;
        private Double percentage;
        private Integer transactionCount;
    }

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalExpenses;
    private List<CategoryBreakdown> breakdown;
}
