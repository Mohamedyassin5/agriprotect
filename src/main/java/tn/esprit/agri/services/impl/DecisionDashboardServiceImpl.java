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
                    .icon("HEALTH")
                    .title("Bonne santé financière — " + healthScore.getOverallScore() + "/100 (" + healthScore.getHealthLevel() + ")")
                    .description(String.format("Revenus : %.0f TND/mois · Dépenses : %.0f TND/mois · Marge nette : %.0f TND/mois",
                            monthlyIncome.doubleValue(), monthlyExpenses.doubleValue(), monthlySavings.doubleValue()))
                    .severity("SUCCESS").build());
        } else {
            insights.add(DecisionDashboardResponse.KeyInsight.builder()
                    .icon("HEALTH")
                    .title("Santé financière à améliorer — " + healthScore.getOverallScore() + "/100 (" + healthScore.getHealthLevel() + ")")
                    .description(String.format("%d point(s) critique(s) à corriger. Revenus : %.0f TND/mois · Dépenses : %.0f TND/mois · Marge : %.0f TND/mois.",
                            healthScore.getRecommendations() != null ? healthScore.getRecommendations().size() : 0,
                            monthlyIncome.doubleValue(), monthlyExpenses.doubleValue(), monthlySavings.doubleValue()))
                    .severity("WARNING").build());
        }

        if (monthlySavings.compareTo(BigDecimal.ZERO) > 0) {
            double savingsRate = monthlySavings.divide(monthlyIncome.max(BigDecimal.ONE), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            BigDecimal target20 = monthlyIncome.multiply(BigDecimal.valueOf(0.20)).setScale(0, RoundingMode.HALF_UP);
            String savingsDetail = savingsRate >= 20
                    ? String.format("Objectif de 20%% atteint ! Vous épargnez %.0f TND/mois.", monthlySavings.doubleValue())
                    : String.format("Vous épargnez %.0f TND/mois. L'objectif de 20%% = %.0f TND/mois — il vous manque %.0f TND/mois.",
                    monthlySavings.doubleValue(), target20.doubleValue(), target20.subtract(monthlySavings).doubleValue());
            insights.add(DecisionDashboardResponse.KeyInsight.builder()
                    .icon("SAVINGS")
                    .title(String.format("Taux d'épargne : %.0f%%", savingsRate))
                    .description(savingsDetail)
                    .severity(savingsRate >= 20 ? "SUCCESS" : "INFO").build());
        } else {
            insights.add(DecisionDashboardResponse.KeyInsight.builder()
                    .icon("SAVINGS")
                    .title(String.format("Déficit de %.0f TND/mois", monthlySavings.abs().doubleValue()))
                    .description(String.format("Dépenses (%.0f TND) supérieures aux revenus (%.0f TND) de %.0f TND/mois. " +
                                    "Réduire les postes non essentiels est la priorité absolue avant tout autre objectif.",
                            monthlyExpenses.doubleValue(), monthlyIncome.doubleValue(), monthlySavings.abs().doubleValue()))
                    .severity("ALERT").build());
        }

        if (emergencyCoverage < 3) {
            BigDecimal fundTarget = monthlyExpenses.multiply(BigDecimal.valueOf(3)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal fundDeficit = fundTarget.subtract(savingsBalance).max(BigDecimal.ZERO);
            insights.add(DecisionDashboardResponse.KeyInsight.builder()
                    .icon("EMERGENCY")
                    .title(String.format("Fonds d'urgence : %.1f mois / 3 mois minimum", emergencyCoverage))
                    .description(String.format("Épargne actuelle : %.0f TND · Objectif minimum : %.0f TND (3 × %.0f TND/mois) · Manque : %.0f TND.",
                            savingsBalance.doubleValue(), fundTarget.doubleValue(), monthlyExpenses.doubleValue(), fundDeficit.doubleValue()))
                    .severity("WARNING").build());
        }

        if ("DECLINING".equals(profitTrend)) {
            BigDecimal profitDrop = firstMonth.subtract(lastMonth);
            insights.add(DecisionDashboardResponse.KeyInsight.builder()
                    .icon("TREND")
                    .title("Rentabilité en baisse sur les 3 derniers mois")
                    .description(String.format("Profit du mois le plus ancien : %.0f TND → profit du mois le plus récent : %.0f TND (recul de %.0f TND). " +
                                    "Identifiez la catégorie de dépenses qui a le plus augmenté.",
                            firstMonth.doubleValue(), lastMonth.doubleValue(), profitDrop.abs().doubleValue()))
                    .severity("WARNING").build());
        }

        // Prioritized actions
        List<DecisionDashboardResponse.ActionItem> actions = new ArrayList<>();
        int priority = 1;

        if (monthlySavings.compareTo(BigDecimal.ZERO) <= 0) {
            actions.add(DecisionDashboardResponse.ActionItem.builder()
                    .priority(priority++)
                    .action(String.format("Réduire les dépenses de %.0f TND/mois pour atteindre l'équilibre " +
                                    "(revenus : %.0f TND, dépenses actuelles : %.0f TND)",
                            monthlySavings.abs().doubleValue(), monthlyIncome.doubleValue(), monthlyExpenses.doubleValue()))
                    .impact("CRITIQUE — l'exploitation est en déficit financier")
                    .category("EXPENSES").build());
        }
        if (emergencyCoverage < 3) {
            BigDecimal fundTarget = monthlyExpenses.multiply(BigDecimal.valueOf(3)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal fundDeficit = fundTarget.subtract(savingsBalance).max(BigDecimal.ZERO);
            BigDecimal monthlyNeeded = fundDeficit.divide(BigDecimal.valueOf(6), 0, RoundingMode.CEILING);
            actions.add(DecisionDashboardResponse.ActionItem.builder()
                    .priority(priority++)
                    .action(String.format("Constituer un fonds d'urgence : il manque %.0f TND (actuel : %.0f TND, cible : %.0f TND). " +
                                    "Épargnez %.0f TND/mois pendant 6 mois pour atteindre le minimum.",
                            fundDeficit.doubleValue(), savingsBalance.doubleValue(), fundTarget.doubleValue(), monthlyNeeded.doubleValue()))
                    .impact("HAUTE — protection contre pannes, sécheresse et imprévus agricoles")
                    .category("SAVINGS").build());
        }
        if (healthScore.getExpenseRatio() != null && healthScore.getExpenseRatio().getScore() < 60) {
            double ratio = monthlyIncome.compareTo(BigDecimal.ZERO) > 0
                    ? monthlyExpenses.divide(monthlyIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;
            BigDecimal targetExpense = monthlyIncome.multiply(BigDecimal.valueOf(0.70)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal reductionNeeded = monthlyExpenses.subtract(targetExpense).max(BigDecimal.ZERO);
            actions.add(DecisionDashboardResponse.ActionItem.builder()
                    .priority(priority++)
                    .action(String.format("Réduire les dépenses de %.0f TND/mois pour passer de %.0f%% à 70%% du ratio dépenses/revenus " +
                                    "(objectif : %.0f TND/mois au lieu de %.0f TND).",
                            reductionNeeded.doubleValue(), ratio, targetExpense.doubleValue(), monthlyExpenses.doubleValue()))
                    .impact("MOYENNE — amélioration directe de la marge bénéficiaire")
                    .category("EXPENSES").build());
        }
        if (healthScore.getDiversification() != null && healthScore.getDiversification().getScore() < 50) {
            actions.add(DecisionDashboardResponse.ActionItem.builder()
                    .priority(priority++)
                    .action("Diversifier les revenus au-delà des ventes (SALES) : services agricoles à d'autres exploitants, " +
                            "location de matériel, cultures de niche, ou vente directe au marché local.")
                    .impact("MOYENNE — réduction du risque lié à une seule culture ou un seul acheteur")
                    .category("INCOME").build());
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
                    String justification = getOptimizationJustification(cat, reductionPct, current, reduction);
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
            strategy = String.format(
                    "Situation critique : flux mensuel négatif de %.0f TND (revenus : %.0f TND · dépenses : %.0f TND). " +
                            "En appliquant les %.0f TND d'économies identifiées, votre flux deviendrait %.0f TND/mois. " +
                            "Priorité immédiate : TRANSPORT et OTHER (les plus réductibles sans impact sur la production).",
                    currentNet.abs().doubleValue(), monthlyIncome.doubleValue(), monthlyExpenses.doubleValue(),
                    totalPotentialSaving.doubleValue(), optimizedNet.doubleValue());
        } else if (currentNet.compareTo(idealSavings) < 0) {
            double optimizedRate = monthlyIncome.compareTo(BigDecimal.ZERO) > 0
                    ? optimizedNet.divide(monthlyIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;
            strategy = String.format(
                    "Flux positif de %.0f TND/mois, mais l'objectif d'épargne (20%% = %.0f TND/mois) n'est pas atteint. " +
                            "Les %.0f TND d'économies identifiées porteraient votre épargne mensuelle à %.0f TND/mois (%.0f%% de vos revenus).",
                    currentNet.doubleValue(), idealSavings.doubleValue(),
                    totalPotentialSaving.doubleValue(), optimizedNet.doubleValue(), optimizedRate);
        } else {
            double currentRate = monthlyIncome.compareTo(BigDecimal.ZERO) > 0
                    ? currentNet.divide(monthlyIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;
            strategy = String.format(
                    "Excellent flux de trésorerie : %.0f TND/mois net (%.0f%% de vos revenus de %.0f TND). " +
                            "Les %.0f TND d'économies supplémentaires détectées pourraient financer une expansion de votre exploitation " +
                            "ou accélérer vos objectifs d'épargne.",
                    currentNet.doubleValue(), currentRate, monthlyIncome.doubleValue(), totalPotentialSaving.doubleValue());
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

    private String getOptimizationJustification(EntryCategory category, double reductionPct, BigDecimal current, BigDecimal saving) {
        return switch (category) {
            case TRANSPORT -> String.format(
                    "Regroupez vos livraisons et optimisez vos trajets : %.0f TND/mois actuellement. " +
                            "En planifiant les livraisons 2×/semaine au lieu de chaque jour, économie estimée à %.0f TND/mois (-%.0f%%).",
                    current.doubleValue(), saving.doubleValue(), reductionPct * 100);
            case OTHER -> String.format(
                    "Frais divers : %.0f TND/mois. Analysez chaque poste : beaucoup de ces dépenses peuvent être " +
                            "reportées ou évitées avec une validation préalable. Économie possible : %.0f TND/mois (-%.0f%%).",
                    current.doubleValue(), saving.doubleValue(), reductionPct * 100);
            case LABOR -> String.format(
                    "Main d'œuvre : %.0f TND/mois. Regrouper les tâches saisonnières et éviter les heures creuses " +
                            "peut économiser %.0f TND/mois (-%.0f%%) sans réduire la productivité.",
                    current.doubleValue(), saving.doubleValue(), reductionPct * 100);
            case EQUIPMENT -> String.format(
                    "Équipements : %.0f TND/mois. La maintenance préventive régulière (vidange, graissage, vérifications) " +
                            "réduit les pannes coûteuses — économie estimée à %.0f TND/mois (-%.0f%%).",
                    current.doubleValue(), saving.doubleValue(), reductionPct * 100);
            case FERTILIZER -> String.format(
                    "Engrais : %.0f TND/mois. Une analyse de sol précise permet d'éviter la sur-fertilisation " +
                            "et d'adapter les doses au besoin réel — économie possible de %.0f TND/mois (-%.0f%%) sans perte de rendement.",
                    current.doubleValue(), saving.doubleValue(), reductionPct * 100);
            case SEEDS -> String.format(
                    "Semences : %.0f TND/mois. Commandes groupées en pré-saison auprès de plusieurs fournisseurs " +
                            "et achat en vrac permettent d'économiser %.0f TND/mois (-%.0f%%).",
                    current.doubleValue(), saving.doubleValue(), reductionPct * 100);
            default -> String.format(
                    "%.0f TND/mois dans cette catégorie — économie de %.0f TND/mois (-%.0f%%) identifiée " +
                            "par rapport aux benchmarks agricoles.",
                    current.doubleValue(), saving.doubleValue(), reductionPct * 100);
        };
    }
}
