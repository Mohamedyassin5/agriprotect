package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Recommendation {
        private String category;
        private String recommendation;
        private String priority;
        private BigDecimal potentialSavings;
    }

    private BigDecimal currentSavingsBalance;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal recommendedMonthlySavings;
    private List<Recommendation> recommendations;
}
