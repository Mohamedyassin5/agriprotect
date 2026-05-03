package tn.esprit.agri.DTO;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class FarmerDetailResponse {

    // ── Identity ──────────────────────────────────────────────
    private String userId;
    private String firstName;
    private String lastName;
    private String email;

    // ── Health ────────────────────────────────────────────────
    private int    healthScore;
    private String healthLevel;
    private String riskLevel;

    // ── Monthly averages (last 3 months) ─────────────────────
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal monthlySavings;

    // ── Totals (all time) ─────────────────────────────────────
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;

    // ── Savings account ───────────────────────────────────────
    private String     savingsAccountName;
    private BigDecimal savingsBalance;
    private BigDecimal savingsGoalAmount;
    private String     savingsGoalTitle;
    private BigDecimal monthlySavingsTarget;
    private String     savingsStatus;
    private Double     goalProgressPct;

    // ── Monthly breakdown (last 6 months) ────────────────────
    private List<MonthlyBreakdown> monthlyBreakdown;

    // ── Recent entries (last 5) ───────────────────────────────
    private List<RecentEntry> recentEntries;

    // ── Active budgets ────────────────────────────────────────
    private List<BudgetDetail> activeBudgets;
    private int activeBudgetAlerts;

    // ── Category breakdown (expenses) ────────────────────────
    private List<CategoryBreakdown> expensesByCategory;

    // ═══════════════════════════════════════
    @Data @Builder
    public static class MonthlyBreakdown {
        private String     month;      // "2025-01"
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal net;
    }

    @Data @Builder
    public static class RecentEntry {
        private Long       id;
        private String     type;       // INCOME / EXPENSE
        private String     category;
        private BigDecimal amount;
        private String     description;
        private LocalDate  date;
    }

    @Data @Builder
    public static class BudgetDetail {
        private Long       id;
        private String     category;
        private BigDecimal plannedAmount;
        private BigDecimal actualSpent;
        private BigDecimal remaining;
        private boolean    overBudget;
        private LocalDate  periodStart;
        private LocalDate  periodEnd;
        private String     periodType;
    }

    @Data @Builder
    public static class CategoryBreakdown {
        private String     category;
        private BigDecimal amount;
        private double     pct;
    }
}
