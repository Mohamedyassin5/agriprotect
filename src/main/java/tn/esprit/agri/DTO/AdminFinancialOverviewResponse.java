package tn.esprit.agri.DTO;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdminFinancialOverviewResponse {

    // ── KPIs plateforme ──────────────────────────────────
    private int totalFarmers;
    private BigDecimal totalSavingsBalance;
    private double averageHealthScore;
    private int farmersAtRisk;       // score < 35
    private int farmersOnTrack;      // score >= 65
    private int farmersInDeficit;    // monthlySavings < 0

    // ── Distribution santé financière ────────────────────
    private Map<String, Integer> healthDistribution;

    // ── Détail par agriculteur ────────────────────────────
    private List<FarmerFinancialSummary> farmers;

    @Data
    @Builder
    public static class FarmerFinancialSummary {
        private String userId;
        private String firstName;
        private String lastName;
        private String email;
        private int healthScore;
        private String healthLevel;
        private BigDecimal monthlyIncome;
        private BigDecimal monthlyExpenses;
        private BigDecimal monthlySavings;
        private BigDecimal savingsBalance;
        private int activeBudgetAlerts;
        private String riskLevel;
    }
}
