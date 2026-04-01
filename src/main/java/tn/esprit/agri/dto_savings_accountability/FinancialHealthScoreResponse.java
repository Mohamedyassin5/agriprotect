package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialHealthScoreResponse {

    private int overallScore;          // 0–100
    private String healthLevel;        // EXCELLENT, GOOD, FAIR, POOR, CRITICAL

    private MetricDetail expenseRatio;
    private MetricDetail incomeRegularity;
    private MetricDetail diversification;
    private MetricDetail savingsRate;
    private MetricDetail trendScore;

    private List<String> recommendations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetricDetail {
        private String name;
        private int score;           // 0–100
        private double weight;
        private String description;
    }
}
