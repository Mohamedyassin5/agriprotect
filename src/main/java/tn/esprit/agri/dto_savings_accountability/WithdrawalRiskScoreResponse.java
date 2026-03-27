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
public class WithdrawalRiskScoreResponse {

    private BigDecimal withdrawalAmount;
    private int riskScore;           // 1-10
    private String riskLevel;        // LOW, MEDIUM, HIGH, CRITICAL
    private String recommendation;

    private RiskFactor goalImpact;
    private RiskFactor expenseCoverage;
    private RiskFactor bufferAnalysis;
    private RiskFactor savingsHistory;

    private List<String> warnings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskFactor {
        private String name;
        private int score;          // 1-10
        private double weight;
        private String detail;
    }
}
