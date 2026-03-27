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
public class ProfitabilityTrendResponse {

    private List<MonthlyProfit> monthlyProfits;
    private BigDecimal overallGrowthRate;
    private String overallTrend;          // GROWING, DECLINING, STABLE
    private String bestMonth;
    private BigDecimal bestMonthProfit;
    private String worstMonth;
    private BigDecimal worstMonthProfit;
    private BigDecimal averageMonthlyProfit;
    private LinearRegressionResult regression;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyProfit {
        private String month;
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal netProfit;
        private BigDecimal profitMargin;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LinearRegressionResult {
        private double slope;
        private double intercept;
        private double rSquared;
        private String interpretation;
    }
}
