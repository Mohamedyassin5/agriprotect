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
public class AlertsResponse {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Alert {
        private String type;
        private String severity;
        private String message;
        private BigDecimal value;
    }

    private List<Alert> alerts;
    private Integer totalAlerts;
}
