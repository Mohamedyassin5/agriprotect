package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.agri.entities.enums.EntryCategory;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseForecastResponse {

    private int forecastMonths;
    private BigDecimal totalForecastedExpenses;
    private List<MonthlyForecast> monthlyForecasts;
    private List<CategoryForecast> categoryForecasts;
    private String methodology;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyForecast {
        private String month;
        private BigDecimal projectedExpenses;
        private BigDecimal confidence;
        private String trend;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryForecast {
        private EntryCategory category;
        private BigDecimal currentMonthlyAvg;
        private BigDecimal forecastedAmount;
        private double growthRate;
        private String trend;
    }
}
