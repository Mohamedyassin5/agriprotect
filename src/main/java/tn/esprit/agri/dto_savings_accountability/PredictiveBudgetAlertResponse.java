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
public class PredictiveBudgetAlertResponse {

    private List<PredictiveAlert> alerts;
    private int totalAlerts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PredictiveAlert {
        private EntryCategory category;
        private BigDecimal budgetedAmount;
        private BigDecimal spentSoFar;
        private BigDecimal dailyBurnRate;
        private BigDecimal projectedMonthEnd;
        private BigDecimal projectedOverspend;
        private int daysRemaining;
        private LocalDate estimatedExhaustionDate;
        private String severity;     // CRITICAL, HIGH, MEDIUM, LOW
        private String recommendation;
    }
}
