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
public class CashflowForecastResponse {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyForecast {
        private String month;
        private LocalDate periodStart;
        private BigDecimal projectedIncome;
        private BigDecimal projectedExpenses;
        private BigDecimal projectedNetCashflow;
        private BigDecimal cumulativeCashflow;
    }

    private BigDecimal currentBalance;
    private BigDecimal averageMonthlyIncome;
    private BigDecimal averageMonthlyExpenses;
    private List<MonthlyForecast> forecasts;
}
