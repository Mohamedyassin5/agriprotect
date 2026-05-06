package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.DTO.AdminFinancialOverviewResponse;
import tn.esprit.agri.DTO.FarmerDetailResponse;
import tn.esprit.agri.dto_savings_accountability.FinancialHealthScoreResponse;
import tn.esprit.agri.dto_savings_accountability.PredictiveBudgetAlertResponse;
import tn.esprit.agri.entities.AccountingEntry;
import tn.esprit.agri.entities.Budget;
import tn.esprit.agri.entities.SavingsAccount;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.EntryCategory;
import tn.esprit.agri.entities.enums.EntryType;
import tn.esprit.agri.entities.enums.Role;
import tn.esprit.agri.repositories.AccountingEntryRepository;
import tn.esprit.agri.repositories.BudgetRepository;
import tn.esprit.agri.repositories.SavingsAccountRepository;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.IAccountingAIService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/financial")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminFinancialController {

    private final UserRepository userRepository;
    private final IAccountingAIService accountingAIService;
    private final SavingsAccountRepository savingsAccountRepository;
    private final AccountingEntryRepository entryRepository;
    private final BudgetRepository budgetRepository;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminFinancialOverviewResponse> getFinancialOverview() {

        List<User> farmers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.FARMER)
                .collect(Collectors.toList());

        LocalDate now = LocalDate.now();
        LocalDate threeMonthsAgo = now.minusMonths(3);

        List<AdminFinancialOverviewResponse.FarmerFinancialSummary> summaries = new ArrayList<>();
        Map<String, Integer> healthDist = new LinkedHashMap<>();
        for (String level : List.of("EXCELLENT", "GOOD", "FAIR", "POOR", "CRITICAL")) {
            healthDist.put(level, 0);
        }

        BigDecimal totalSavings = BigDecimal.ZERO;
        double totalScore = 0;
        int atRisk = 0;
        int onTrack = 0;
        int inDeficit = 0;

        for (User farmer : farmers) {
            try {
                FinancialHealthScoreResponse health = accountingAIService.getFinancialHealthScore(farmer.getId());

                BigDecimal income3m = entryRepository.sumAmountByUserIdAndTypeAndDateRange(
                        farmer.getId(), EntryType.INCOME, threeMonthsAgo, now);
                BigDecimal expense3m = entryRepository.sumAmountByUserIdAndTypeAndDateRange(
                        farmer.getId(), EntryType.EXPENSE, threeMonthsAgo, now);

                BigDecimal monthlyIncome = income3m.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
                BigDecimal monthlyExpenses = expense3m.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
                BigDecimal monthlySavings = monthlyIncome.subtract(monthlyExpenses);

                BigDecimal savingsBalance = savingsAccountRepository.findByUserId(farmer.getId())
                        .map(s -> s.getCurrentBalance())
                        .orElse(BigDecimal.ZERO);

                PredictiveBudgetAlertResponse alerts = accountingAIService.getPredictiveBudgetAlerts(farmer.getId());
                int alertCount = alerts.getAlerts() != null ? alerts.getAlerts().size() : 0;

                int score = health.getOverallScore();
                String level = health.getHealthLevel();
                String riskLevel = score < 35 ? "HIGH" : score < 65 ? "MEDIUM" : "LOW";

                totalSavings = totalSavings.add(savingsBalance);
                totalScore += score;
                healthDist.merge(level, 1, Integer::sum);

                if (score < 35) atRisk++;
                if (score >= 65) onTrack++;
                if (monthlySavings.compareTo(BigDecimal.ZERO) < 0) inDeficit++;

                summaries.add(AdminFinancialOverviewResponse.FarmerFinancialSummary.builder()
                        .userId(farmer.getId())
                        .firstName(farmer.getFirstName())
                        .lastName(farmer.getLastName())
                        .email(farmer.getEmail())
                        .healthScore(score)
                        .healthLevel(level)
                        .monthlyIncome(monthlyIncome)
                        .monthlyExpenses(monthlyExpenses)
                        .monthlySavings(monthlySavings)
                        .savingsBalance(savingsBalance)
                        .activeBudgetAlerts(alertCount)
                        .riskLevel(riskLevel)
                        .build());

            } catch (Exception e) {
                log.warn("Erreur calcul pour farmer {}: {}", farmer.getId(), e.getMessage());
            }
        }

        summaries.sort(Comparator.comparingInt(AdminFinancialOverviewResponse.FarmerFinancialSummary::getHealthScore));

        double avgScore = farmers.isEmpty() ? 0 : totalScore / farmers.size();

        return ResponseEntity.ok(AdminFinancialOverviewResponse.builder()
                .totalFarmers(farmers.size())
                .totalSavingsBalance(totalSavings)
                .averageHealthScore(Math.round(avgScore * 10.0) / 10.0)
                .farmersAtRisk(atRisk)
                .farmersOnTrack(onTrack)
                .farmersInDeficit(inDeficit)
                .healthDistribution(healthDist)
                .farmers(summaries)
                .build());
    }

    @GetMapping("/farmer/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FarmerDetailResponse> getFarmerDetail(@PathVariable String userId) {

        User farmer = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Farmer not found: " + userId));

        LocalDate now        = LocalDate.now();
        LocalDate threeMonthsAgo = now.minusMonths(3);
        LocalDate sixMonthsAgo   = now.minusMonths(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        // ── Health score ──────────────────────────────────────────────
        FinancialHealthScoreResponse health = accountingAIService.getFinancialHealthScore(userId);
        PredictiveBudgetAlertResponse alerts = accountingAIService.getPredictiveBudgetAlerts(userId);
        int alertCount = alerts.getAlerts() != null ? alerts.getAlerts().size() : 0;

        // ── Monthly averages (3 months) ───────────────────────────────
        BigDecimal income3m   = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.INCOME,  threeMonthsAgo, now);
        BigDecimal expense3m  = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, threeMonthsAgo, now);
        BigDecimal monthlyIncome   = income3m.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyExpenses = expense3m.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal monthlySavings  = monthlyIncome.subtract(monthlyExpenses);

        // ── All-time totals ───────────────────────────────────────────
        BigDecimal totalIncome   = entryRepository.sumAmountByUserIdAndType(userId, EntryType.INCOME);
        BigDecimal totalExpenses = entryRepository.sumAmountByUserIdAndType(userId, EntryType.EXPENSE);

        // ── Savings account ───────────────────────────────────────────
        Optional<SavingsAccount> savingsOpt = savingsAccountRepository.findByUserId(userId);
        BigDecimal savingsBalance = savingsOpt.map(SavingsAccount::getCurrentBalance).orElse(BigDecimal.ZERO);

        Double goalPct = null;
        String accountName = null, goalTitle = null, savingsStatus = null;
        BigDecimal goalAmount = null, savingsTarget = null;

        if (savingsOpt.isPresent()) {
            SavingsAccount sa = savingsOpt.get();
            accountName   = sa.getAccountName();
            goalTitle     = sa.getGoalTitle();
            goalAmount    = sa.getGoalAmount();
            savingsTarget = sa.getMonthlySavingsTarget();
            savingsStatus = sa.getStatus() != null ? sa.getStatus().name() : null;
            if (goalAmount != null && goalAmount.compareTo(BigDecimal.ZERO) > 0) {
                goalPct = savingsBalance.divide(goalAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
            }
        }

        // ── Monthly breakdown (6 months) ──────────────────────────────
        List<FarmerDetailResponse.MonthlyBreakdown> breakdown = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd   = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            BigDecimal inc = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.INCOME,  monthStart, monthEnd);
            BigDecimal exp = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, monthStart, monthEnd);
            breakdown.add(FarmerDetailResponse.MonthlyBreakdown.builder()
                    .month(monthStart.format(fmt))
                    .income(inc)
                    .expenses(exp)
                    .net(inc.subtract(exp))
                    .build());
        }

        // ── Recent entries (last 5) ───────────────────────────────────
        List<FarmerDetailResponse.RecentEntry> recentEntries = entryRepository
                .findByUserIdOrderByEntryDateDesc(userId).stream()
                .limit(5)
                .map(e -> FarmerDetailResponse.RecentEntry.builder()
                        .id(e.getId())
                        .type(e.getEntryType().name())
                        .category(e.getCategory().name())
                        .amount(e.getAmount())
                        .description(e.getDescription())
                        .date(e.getEntryDate())
                        .build())
                .collect(Collectors.toList());

        // ── Active budgets with overspend ─────────────────────────────
        List<Budget> budgets = budgetRepository.findActiveBudgets(userId, now, null);
        List<FarmerDetailResponse.BudgetDetail> budgetDetails = new ArrayList<>();
        for (Budget b : budgets) {
            BigDecimal actual = entryRepository.sumAmountByUserIdAndTypeAndDateRange(
                    userId, EntryType.EXPENSE, b.getPeriodStart(), b.getPeriodEnd());
            BigDecimal remaining = b.getPlannedAmount().subtract(actual);
            budgetDetails.add(FarmerDetailResponse.BudgetDetail.builder()
                    .id(b.getId())
                    .category(b.getCategory().name())
                    .plannedAmount(b.getPlannedAmount())
                    .actualSpent(actual)
                    .remaining(remaining)
                    .overBudget(remaining.compareTo(BigDecimal.ZERO) < 0)
                    .periodStart(b.getPeriodStart())
                    .periodEnd(b.getPeriodEnd())
                    .periodType(b.getPeriodType().name())
                    .build());
        }

        // ── Expenses by category ──────────────────────────────────────
        List<AccountingEntry> allExpenses = entryRepository.findByUserIdAndEntryType(userId, EntryType.EXPENSE);
        Map<EntryCategory, BigDecimal> byCat = new LinkedHashMap<>();
        for (AccountingEntry e : allExpenses) {
            byCat.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
        }
        BigDecimal totalExp = totalExpenses.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : totalExpenses;
        List<FarmerDetailResponse.CategoryBreakdown> catBreakdown = byCat.entrySet().stream()
                .sorted(Map.Entry.<EntryCategory, BigDecimal>comparingByValue().reversed())
                .map(entry -> FarmerDetailResponse.CategoryBreakdown.builder()
                        .category(entry.getKey().name())
                        .amount(entry.getValue())
                        .pct(entry.getValue().divide(totalExp, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue())
                        .build())
                .collect(Collectors.toList());

        int score = health.getOverallScore();
        String riskLevel = score < 35 ? "HIGH" : score < 65 ? "MEDIUM" : "LOW";

        return ResponseEntity.ok(FarmerDetailResponse.builder()
                .userId(userId)
                .firstName(farmer.getFirstName())
                .lastName(farmer.getLastName())
                .email(farmer.getEmail())
                .healthScore(score)
                .healthLevel(health.getHealthLevel())
                .riskLevel(riskLevel)
                .monthlyIncome(monthlyIncome)
                .monthlyExpenses(monthlyExpenses)
                .monthlySavings(monthlySavings)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .savingsAccountName(accountName)
                .savingsBalance(savingsBalance)
                .savingsGoalAmount(goalAmount)
                .savingsGoalTitle(goalTitle)
                .monthlySavingsTarget(savingsTarget)
                .savingsStatus(savingsStatus)
                .goalProgressPct(goalPct)
                .monthlyBreakdown(breakdown)
                .recentEntries(recentEntries)
                .activeBudgets(budgetDetails)
                .activeBudgetAlerts(alertCount)
                .expensesByCategory(catBreakdown)
                .build());
    }
}
