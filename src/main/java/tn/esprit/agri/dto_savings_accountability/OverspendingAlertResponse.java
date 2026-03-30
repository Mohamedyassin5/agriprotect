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
public class OverspendingAlertResponse {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Alert {
        private EntryCategory category;
        private BigDecimal budgetedAmount;
        private BigDecimal actualAmount;
        private BigDecimal overspending;
        private Double percentageOver;
        private String severity;
    }

    private List<Alert> alerts;
    private Integer totalAlertsCount;
}
