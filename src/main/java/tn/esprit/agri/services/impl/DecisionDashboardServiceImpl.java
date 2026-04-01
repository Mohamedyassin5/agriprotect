package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.dto_savings_accountability.*;
import tn.esprit.agri.entities.SavingsAccount;
import tn.esprit.agri.entities.enums.EntryCategory;
import tn.esprit.agri.entities.enums.EntryType;
import tn.esprit.agri.repositories.AccountingEntryRepository;
import tn.esprit.agri.repositories.SavingsAccountRepository;
import tn.esprit.agri.services.IAccountingAIService;
import tn.esprit.agri.services.IDecisionDashboardService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DecisionDashboardServiceImpl implements IDecisionDashboardService {

    private final AccountingEntryRepository entryRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final IAccountingAIService accountingAIService;

    // ==============================
    // Feature 11: Decision Dashboard
    // ==============================
    @Override
    public DecisionDashboardResponse getDashboard(String userId) {
        LocalDate now = LocalDate.now();
        LocalDate threeMonthsAgo = now.minusMonths(3);

        BigDecimal income3m = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.INCOME, threeMonthsAgo, now);
        BigDecimal expense3m = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, threeMonthsAgo, now);
        Long incomeEntries = entryRepository.countByUserIdAndTypeAndDateRange(userId, EntryType.INCOME, threeMonthsAgo, now);
        Long expenseEntries = entryRepository.countByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, threeMonthsAgo, now);

        BigDecimal monthlyIncome = income3m.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyExpenses = expense3m.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

        String dataSource = "Moyennes mensuelles calculées sur " + incomeEntries + " écritures INCOME et "
                + expenseEntries + " écritures EXPENSE du " + threeMonthsAgo + " au " + now + ".";
        BigDecimal monthlySavings = monthlyIncome.subtract(monthlyExpenses);

        // Health score
        FinancialHealthScoreResponse healthScore = accountingAIService.getFinancialHealthScore(userId);

        // Savings balance
        BigDecimal savingsBalance = BigDecimal.ZERO;
        Optional<SavingsAccount> accountOpt = savingsAccountRepository.findByUserId(userId);
        if (accountOpt.isPresent()) {
            savingsBalance = accountOpt.get().getCurrentBalance();
        }

        // Emergency fund coverage
        double emergencyCoverage = monthlyExpenses.compareTo(BigDecimal.ZERO) > 0
                ? savingsBalance.divide(monthlyExpenses, 2, RoundingMode.HALF_UP).doubleValue()
                : 0;

        // Profitability trend
        BigDecimal firstMonth = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.INCOME,
                threeMonthsAgo, threeMonthsAgo.plusMonths(1))
                .subtract(entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE,
                        threeMonthsAgo, threeMonthsAgo.plusMonths(1)));
        BigDecimal lastMonth = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.INCOME,
                now.minusMonths(1), now)
                .subtract(entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE,
                        now.minusMonths(1), now));
        String profitTrend = lastMonth.compareTo(firstMonth) > 0 ? "GROWING"
                : lastMonth.compareTo(firstMonth) < 0 ? "DECLINING" : "STABLE";

        // Key insights
        List<DecisionDashboardResponse.KeyInsight> insights = new ArrayList<>();

        if (healthScore.getOverallScore() >= 70) {
            insights.add(DecisionDashboardResponse.KeyInsight.builder()
                    .icon("HEALTH").title("Bonne santé financière")
                    .description("Votre score de santé financière est de " + healthScore.getOverallScore() + "/100")
                    .severity("SUCCESS").build());
        } else {
            insights.add(DecisionDashboardResponse.KeyInsight.builder()
                    .icon("HEALTH").title("Santé financière à améliorer")
                    .description("Score: " + healthScore.getOverallScore() + "/100 — des actions sont recommandées")
                    .severity("WARNING").build());
        }

        if (monthlySavings.compareTo(BigDecimal.ZERO) > 0) {
            double savingsRate = monthlySavings.divide(monthlyIncome.max(BigDecimal.ONE), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            insights.add(DecisionDashboardResponse.KeyInsight.builder()
                    .icon("SAVINGS").title("Taux d'épargne: " + Math.round(savingsRate) + "%")
                    .description("Vous épargnez " + monthlySavings + " DT par mois en moyenne")
                    .severity(savingsRate >= 20 ? "SUCCESS" : "INFO").build());
        } else {
            insights.add(DecisionDashboardResponse.KeyInsight.builder()
                    .icon("SAVINGS").title("Épargne négative")
                    .description("Vos dépenses dépassent vos revenus — action urgente nécessaire")
                    .severity("ALERT").build());
        }

        if (emergencyCoverage < 3) {
            insights.add(DecisionDashboardResponse.KeyInsight.builder()
                    .icon("EMERGENCY").title("Fonds d'urgence insuffisant")
                    .description("Votre épargne couvre seulement " + emergencyCoverage + " mois de dépenses (minimum recommandé: 3)")
                    .severity("WARNING").build());
        }

        if ("DECLINING".equals(profitTrend)) {
            insights.add(DecisionDashboardResponse.KeyInsight.builder()
                    .icon("TREND").title("Rentabilité en baisse")
                    .description("Votre profit net diminue ces derniers mois — analysez vos dépenses")
                    .severity("WARNING").build());
        }

        // Prioritized actions
        List<DecisionDashboardResponse.ActionItem> actions = new ArrayList<>();
        int priority = 1;

        if (monthlySavings.compareTo(BigDecimal.ZERO) <= 0) {
            actions.add(DecisionDashboardResponse.ActionItem.builder()
                    .priority(priority++).action("Réduisez vos dépenses pour retrouver un solde positif")
                    .impact("Critique — stabilité financière en danger").category("EXPENSES").build());
        }
        if (emergencyCoverage < 3) {
            actions.add(DecisionDashboardResponse.ActionItem.builder()
                    .priority(priority++).action("Constituez un fonds d'urgence de 3 mois minimum")
                    .impact("Haute — protection contre les imprévus").category("SAVINGS").build());
        }
        if (healthScore.getExpenseRatio() != null && healthScore.getExpenseRatio().getScore() < 60) {
            actions.add(DecisionDashboardResponse.ActionItem.builder()
                    .priority(priority++).action("Optimisez votre ratio dépenses/revenus")
                    .impact("Moyenne — amélioration de la marge bénéficiaire").category("EXPENSES").build());
        }
        if (healthScore.getDiversification() != null && healthScore.getDiversification().getScore() < 50) {
            actions.add(DecisionDashboardResponse.ActionItem.builder()
                    .priority(priority++).action("Diversifiez vos sources de revenus")
                    .impact("Moyenne — réduction du risque financier").category("INCOME").build());
        }

        return DecisionDashboardResponse.builder()
                .analysisPeriodFrom(threeMonthsAgo)
                .analysisPeriodTo(now)
                .totalIncomeEntriesAnalyzed(incomeEntries)
                .totalExpenseEntriesAnalyzed(expenseEntries)
                .dataSource(dataSource)
                .financialHealthScore(healthScore.getOverallScore())
                .healthLevel(healthScore.getHealthLevel())
                .monthlyIncome(monthlyIncome)
                .monthlyExpenses(monthlyExpenses)
                .monthlySavings(monthlySavings)
                .savingsBalance(savingsBalance)
                .profitabilityTrend(profitTrend)
                .emergencyFundCoverage(emergencyCoverage)
                .keyInsights(insights)
                .prioritizedActions(actions)
                .build();
    }

    // ==============================
    // Feature 12: Cash Flow Optimizer
    // ==============================
    @Override
    public CashFlowOptimizerResponse optimizeCashFlow(String userId) {
        LocalDate now = LocalDate.now();
        LocalDate threeMonthsAgo = now.minusMonths(3);

        BigDecimal income3m = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.INCOME, threeMonthsAgo, now);
        BigDecimal expense3m = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, threeMonthsAgo, now);
        BigDecimal monthlyIncome = income3m.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyExpenses = expense3m.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal currentNet = monthlyIncome.subtract(monthlyExpenses);

        String dataSource = "Analyse basée sur les écritures réelles du " + threeMonthsAgo + " au " + now
                + ". Chaque catégorie : somme réelle sur 3 mois ÷ 3 = moyenne mensuelle.";

        // Analyze each expense category (real totals + entry count for transparency)
        Map<EntryCategory, BigDecimal> categoryTotals = new LinkedHashMap<>();
        Map<EntryCategory, Integer> categoryEntryCounts = new LinkedHashMap<>();
        for (EntryCategory cat : EntryCategory.values()) {
            BigDecimal catTotal = entryRepository.sumAmountByCategoryAndDateRange(
                    userId, EntryType.EXPENSE, cat, threeMonthsAgo, now);
            int catCount = entryRepository.findByUserIdAndEntryTypeAndCategoryAndEntryDateBetween(
                    userId, EntryType.EXPENSE, cat, threeMonthsAgo, now).size();
            if (catTotal.compareTo(BigDecimal.ZERO) > 0) {
                categoryTotals.put(cat, catTotal);
                categoryEntryCounts.put(cat, catCount);
            }
        }
        // Build monthly averages map for quick lookup
        Map<EntryCategory, BigDecimal> categoryExpenses = new LinkedHashMap<>();
        categoryTotals.forEach((cat, total) ->
                categoryExpenses.put(cat, total.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP)));

        // Optimal allocation rules for agriculture:
        // - Essential (SEEDS, FERTILIZER, IRRIGATION): keep at current or slight reduction
        // - Operational (LABOR, EQUIPMENT): optimize by 10%
        // - Reducible (TRANSPORT, OTHER): optimize by 15-20%
        // - Fixed (INSURANCE, LOAN_PAYMENT): keep as-is
        List<CashFlowOptimizerResponse.OptimizationSuggestion> suggestions = new ArrayList<>();
        BigDecimal totalPotentialSaving = BigDecimal.ZERO;
        int priority = 1;

        Map<EntryCategory, Double> reductionTargets = new LinkedHashMap<>();
        reductionTargets.put(EntryCategory.TRANSPORT, 0.15);
        reductionTargets.put(EntryCategory.OTHER, 0.20);
        reductionTargets.put(EntryCategory.LABOR, 0.10);
        reductionTargets.put(EntryCategory.EQUIPMENT, 0.10);
        reductionTargets.put(EntryCategory.FERTILIZER, 0.05);
        reductionTargets.put(EntryCategory.SEEDS, 0.05);

        for (Map.Entry<EntryCategory, Double> target : reductionTargets.entrySet()) {
            EntryCategory cat = target.getKey();
            double reductionPct = target.getValue();

            if (categoryExpenses.containsKey(cat)) {
                BigDecimal current = categoryExpenses.get(cat);
                BigDecimal reduction = current.multiply(BigDecimal.valueOf(reductionPct)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal suggested = current.subtract(reduction);

                if (reduction.compareTo(BigDecimal.valueOf(5)) > 0) { // Only suggest if savings > 5 DT
                    String justification = getOptimizationJustification(cat, reductionPct);
                    suggestions.add(CashFlowOptimizerResponse.OptimizationSuggestion.builder()
                            .priority(priority++)
                            .category(cat)
                            .type("REDUCE_EXPENSE")
                            .realTotalLast3Months(categoryTotals.get(cat))
                            .numberOfEntries(categoryEntryCounts.getOrDefault(cat, 0))
                            .currentAmount(current)
                            .suggestedAmount(suggested)
                            .potentialSaving(reduction)
                            .justification(justification)
                            .build());
                    totalPotentialSaving = totalPotentialSaving.add(reduction);
                }
            }
        }

        // Ideal allocations
        BigDecimal idealSavings = monthlyIncome.multiply(BigDecimal.valueOf(0.20)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal idealExpensesCeiling = monthlyIncome.multiply(BigDecimal.valueOf(0.80)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal optimizedNet = currentNet.add(totalPotentialSaving);

        String strategy;
        if (currentNet.compareTo(BigDecimal.ZERO) <= 0) {
            strategy = "Priorité absolue : réduire les dépenses pour atteindre un flux de trésorerie positif. " +
                    "Concentrez-vous sur les catégories à fort potentiel d'économie.";
        } else if (currentNet.compareTo(idealSavings) < 0) {
            strategy = "Votre flux est positif mais insuffisant pour une épargne optimale (20% des revenus). " +
                    "Appliquez les optimisations suggérées pour augmenter votre capacité d'épargne.";
        } else {
            strategy = "Excellent flux de trésorerie ! Maintenez vos bonnes habitudes et " +
                    "envisagez d'investir l'excédent dans la croissance de votre exploitation.";
        }

        return CashFlowOptimizerResponse.builder()
                .analysisPeriodFrom(threeMonthsAgo)
                .analysisPeriodTo(now)
                .dataSource(dataSource)
                .monthlyIncome(monthlyIncome)
                .monthlyExpenses(monthlyExpenses)
                .currentMonthlyNet(currentNet)
                .optimizedMonthlyNet(optimizedNet)
                .potentialImprovement(totalPotentialSaving)
                .suggestions(suggestions)
                .idealSavingsAllocation(idealSavings)
                .idealExpensesCeiling(idealExpensesCeiling)
                .overallStrategy(strategy)
                .build();
    }

    private String getOptimizationJustification(EntryCategory category, double reductionPct) {
        int pct = (int) (reductionPct * 100);
        return switch (category) {
            case TRANSPORT -> "Optimisez les trajets et regroupez les livraisons pour réduire de " + pct + "%";
            case OTHER -> "Les frais divers peuvent souvent être réduits de " + pct + "% par une meilleure planification";
            case LABOR -> "Optimisez la planification du travail pour gagner " + pct + "% d'efficacité";
            case EQUIPMENT -> "Entretenez régulièrement pour éviter les réparations coûteuses (-" + pct + "%)";
            case FERTILIZER -> "Utilisez l'analyse de sol pour optimiser les doses et réduire de " + pct + "%";
            case SEEDS -> "Comparez les fournisseurs et achetez en gros pour économiser " + pct + "%";
            default -> "Potentiel d'optimisation de " + pct + "% identifié";
        };
    }
}
