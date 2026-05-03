package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.dto_savings_accountability.*;
import tn.esprit.agri.entities.Budget;
import tn.esprit.agri.entities.enums.EntryCategory;
import tn.esprit.agri.entities.enums.EntryType;
import tn.esprit.agri.repositories.AccountingEntryRepository;
import tn.esprit.agri.repositories.BudgetRepository;
import tn.esprit.agri.services.IAccountingAIService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountingAIServiceImpl implements IAccountingAIService {

    private final AccountingEntryRepository entryRepository;
    private final BudgetRepository budgetRepository;

    // ==============================
    // Feature 1: Financial Health Score
    // ==============================
    @Override
    public FinancialHealthScoreResponse getFinancialHealthScore(String userId) {
        LocalDate now = LocalDate.now();
        LocalDate threeMonthsAgo = now.minusMonths(3);
        LocalDate sixMonthsAgo = now.minusMonths(6);

        BigDecimal income3m = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.INCOME, threeMonthsAgo, now);
        BigDecimal expense3m = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, threeMonthsAgo, now);
        BigDecimal income6m = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.INCOME, sixMonthsAgo, now);
        BigDecimal expense6m = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, sixMonthsAgo, now);

        // Metric 1: Expense Ratio (weight 0.30) — lower is better
        int expenseRatioScore = 50;
        if (income3m.compareTo(BigDecimal.ZERO) > 0) {
            double ratio = expense3m.doubleValue() / income3m.doubleValue();
            if (ratio <= 0.5) expenseRatioScore = 100;
            else if (ratio <= 0.7) expenseRatioScore = 80;
            else if (ratio <= 0.85) expenseRatioScore = 60;
            else if (ratio <= 1.0) expenseRatioScore = 40;
            else expenseRatioScore = 15;
        }

        // Metric 2: Income Regularity (weight 0.20) — consistent monthly income
        Long monthsWithIncome = entryRepository.countDistinctMonthsWithEntries(userId, EntryType.INCOME.name(), threeMonthsAgo, now);
        int incomeRegularityScore = Math.min(100, (int) (monthsWithIncome * 100 / 3));

        // Metric 3: Diversification (weight 0.15) — income from multiple categories
        Long incomeCategories = entryRepository.countDistinctCategoriesByUserIdAndType(userId, EntryType.INCOME);
        int diversificationScore = Math.min(100, (int) (incomeCategories * 25));

        // Metric 4: Savings Rate (weight 0.20) — % of income saved
        int savingsRateScore = 0;
        if (income3m.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal savings = income3m.subtract(expense3m);
            double savingsRate = savings.doubleValue() / income3m.doubleValue();
            if (savingsRate >= 0.3) savingsRateScore = 100;
            else if (savingsRate >= 0.2) savingsRateScore = 80;
            else if (savingsRate >= 0.1) savingsRateScore = 60;
            else if (savingsRate >= 0) savingsRateScore = 40;
            else savingsRateScore = 10;
        }

        // Metric 5: Trend Score (weight 0.15) — is profit improving?
        BigDecimal firstHalfNet = BigDecimal.ZERO;
        BigDecimal secondHalfNet = income3m.subtract(expense3m);
        int trendScore = 50;
        if (income6m.compareTo(BigDecimal.ZERO) > 0 && expense6m.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal firstHalfIncome = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.INCOME, sixMonthsAgo, threeMonthsAgo);
            BigDecimal firstHalfExpense = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, sixMonthsAgo, threeMonthsAgo);
            firstHalfNet = firstHalfIncome.subtract(firstHalfExpense);

            if (secondHalfNet.compareTo(firstHalfNet) > 0) trendScore = 85;
            else if (secondHalfNet.compareTo(firstHalfNet) == 0) trendScore = 50;
            else trendScore = 25;
        }

        // Weighted overall
        double overall = expenseRatioScore * 0.30 + incomeRegularityScore * 0.20
                + diversificationScore * 0.15 + savingsRateScore * 0.20 + trendScore * 0.15;
        int overallScore = (int) Math.round(overall);

        String healthLevel;
        if (overallScore >= 80) healthLevel = "EXCELLENT";
        else if (overallScore >= 65) healthLevel = "GOOD";
        else if (overallScore >= 50) healthLevel = "FAIR";
        else if (overallScore >= 35) healthLevel = "POOR";
        else healthLevel = "CRITICAL";

        BigDecimal monthlyIncome3m = income3m.divide(BigDecimal.valueOf(3), 0, RoundingMode.HALF_UP);
        BigDecimal monthlyExpense3m = expense3m.divide(BigDecimal.valueOf(3), 0, RoundingMode.HALF_UP);

        List<String> recommendations = new ArrayList<>();

        if (expenseRatioScore < 60) {
            double ratioDisplay = income3m.compareTo(BigDecimal.ZERO) > 0
                    ? expense3m.doubleValue() / income3m.doubleValue() * 100 : 0;
            BigDecimal targetExpense = monthlyIncome3m.multiply(BigDecimal.valueOf(0.70));
            BigDecimal monthlyReduction = monthlyExpense3m.subtract(targetExpense).max(BigDecimal.ZERO);
            recommendations.add(String.format(
                    "Vos dépenses (%.0f TND/mois) représentent %.0f%% de vos revenus (%.0f TND/mois). " +
                    "Pour un ratio sain de 70%%, vous devez réduire de %.0f TND/mois — " +
                    "examinez en priorité FERTILIZER, LABOR et EQUIPMENT qui concentrent généralement les plus grands postes.",
                    monthlyExpense3m.doubleValue(), ratioDisplay, monthlyIncome3m.doubleValue(), monthlyReduction.doubleValue()));
        }

        if (incomeRegularityScore < 70) {
            recommendations.add(String.format(
                    "Revenus présents seulement %d mois sur 3 analysés : votre exploitation a des périodes sans entrée d'argent. " +
                    "Planifiez des ventes échelonnées ou des contrats récurrents pour assurer un revenu chaque mois, " +
                    "même en dehors des grandes récoltes.",
                    monthsWithIncome));
        }

        if (diversificationScore < 50) {
            recommendations.add(String.format(
                    "Seulement %d catégorie(s) de revenus détectée(s) — forte dépendance à une source unique. " +
                    "Ajoutez au moins 2 sources supplémentaires : ventes directes de plusieurs cultures, " +
                    "location de matériel agricole, ou prestation de services à d'autres agriculteurs.",
                    incomeCategories));
        }

        if (savingsRateScore < 60) {
            BigDecimal monthlySavings3m = income3m.subtract(expense3m).divide(BigDecimal.valueOf(3), 0, RoundingMode.HALF_UP);
            if (monthlySavings3m.compareTo(BigDecimal.ZERO) < 0) {
                recommendations.add(String.format(
                        "DÉFICIT : vos dépenses dépassent vos revenus de %.0f TND/mois en moyenne. " +
                        "Priorité absolue — identifiez les 2-3 catégories les plus élevées et réduisez-les immédiatement " +
                        "pour retrouver un flux positif avant de penser à l'épargne.",
                        monthlySavings3m.abs().doubleValue()));
            } else {
                BigDecimal targetSavings = monthlyIncome3m.multiply(BigDecimal.valueOf(0.20));
                BigDecimal gap = targetSavings.subtract(monthlySavings3m).max(BigDecimal.ZERO);
                double currentRate = income3m.compareTo(BigDecimal.ZERO) > 0
                        ? income3m.subtract(expense3m).doubleValue() / income3m.doubleValue() * 100 : 0;
                recommendations.add(String.format(
                        "Taux d'épargne actuel : %.0f%% (%.0f TND/mois). L'objectif est 20%% = %.0f TND/mois. " +
                        "Il vous manque %.0f TND/mois — réduire TRANSPORT ou OTHER de quelques dépenses " +
                        "non essentielles peut suffire à combler cet écart.",
                        currentRate, monthlySavings3m.doubleValue(), targetSavings.doubleValue(), gap.doubleValue()));
            }
        }

        if (trendScore < 50) {
            BigDecimal decline = firstHalfNet.subtract(secondHalfNet);
            recommendations.add(String.format(
                    "Rentabilité en recul : profit des mois 4-6 (%.0f TND) vs mois 1-3 (%.0f TND) — " +
                    "baisse de %.0f TND sur 6 mois. Comparez vos écritures de dépenses sur ces deux périodes " +
                    "pour identifier la catégorie dont les coûts ont le plus augmenté.",
                    firstHalfNet.doubleValue(), secondHalfNet.doubleValue(), decline.abs().doubleValue()));
        }

        return FinancialHealthScoreResponse.builder()
                .overallScore(overallScore)
                .healthLevel(healthLevel)
                .expenseRatio(buildMetric("Ratio Dépenses/Revenus", expenseRatioScore, 0.30, "Mesure la part de revenus consommée par les dépenses"))
                .incomeRegularity(buildMetric("Régularité des Revenus", incomeRegularityScore, 0.20, "Vérifie la présence de revenus chaque mois"))
                .diversification(buildMetric("Diversification", diversificationScore, 0.15, "Nombre de catégories de revenus différentes"))
                .savingsRate(buildMetric("Taux d'Épargne", savingsRateScore, 0.20, "Pourcentage de revenus non dépensés"))
                .trendScore(buildMetric("Tendance", trendScore, 0.15, "Évolution de la rentabilité sur 6 mois"))
                .recommendations(recommendations)
                .build();
    }

    private FinancialHealthScoreResponse.MetricDetail buildMetric(String name, int score, double weight, String desc) {
        return FinancialHealthScoreResponse.MetricDetail.builder()
                .name(name).score(score).weight(weight).description(desc).build();
    }

    // ==============================
    // Feature 2: AI Expense Forecasting
    // ==============================
    @Override
    public ExpenseForecastResponse getExpenseForecast(String userId, Integer months) {
        int forecastMonths = months != null ? months : 3;
        LocalDate now = LocalDate.now();

        // Weighted moving average per category
        List<ExpenseForecastResponse.CategoryForecast> categoryForecasts = new ArrayList<>();
        BigDecimal totalForecasted = BigDecimal.ZERO;

        for (EntryCategory category : EntryCategory.values()) {
            BigDecimal[] monthlyAmounts = new BigDecimal[7]; // 6 past months + current month
            boolean hasData = false;

            for (int i = 0; i < 7; i++) {
                LocalDate monthStart = now.minusMonths(6 - i).withDayOfMonth(1);
                LocalDate monthEnd = (i == 6) ? now : monthStart.plusMonths(1).minusDays(1); // current month uses today
                monthlyAmounts[i] = entryRepository.sumAmountByCategoryAndDateRange(
                        userId, EntryType.EXPENSE, category, monthStart, monthEnd);
                if (monthlyAmounts[i].compareTo(BigDecimal.ZERO) > 0) hasData = true;
            }

            if (!hasData) continue;

            // Weighted moving average: recent months weighted more (1,1,2,2,3,3,4)
            double[] weights = {1, 1, 2, 2, 3, 3, 4};
            double weightedSum = 0;
            double totalWeight = 0;
            for (int i = 0; i < 7; i++) {
                weightedSum += monthlyAmounts[i].doubleValue() * weights[i];
                totalWeight += weights[i];
            }
            BigDecimal forecast = BigDecimal.valueOf(weightedSum / totalWeight).setScale(2, RoundingMode.HALF_UP);

            // Growth rate (first 3 months vs last 4 months including current)
            BigDecimal firstHalf = monthlyAmounts[0].add(monthlyAmounts[1]).add(monthlyAmounts[2]);
            BigDecimal secondHalf = monthlyAmounts[3].add(monthlyAmounts[4]).add(monthlyAmounts[5]).add(monthlyAmounts[6]);
            double growthRate = firstHalf.compareTo(BigDecimal.ZERO) > 0
                    ? secondHalf.subtract(firstHalf).doubleValue() / firstHalf.doubleValue() * 100
                    : 0;

            String trend = growthRate > 5 ? "INCREASING" : growthRate < -5 ? "DECREASING" : "STABLE";

            BigDecimal currentAvg = secondHalf.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);

            categoryForecasts.add(ExpenseForecastResponse.CategoryForecast.builder()
                    .category(category)
                    .currentMonthlyAvg(currentAvg)
                    .forecastedAmount(forecast)
                    .growthRate(Math.round(growthRate * 100.0) / 100.0)
                    .trend(trend)
                    .build());

            totalForecasted = totalForecasted.add(forecast.multiply(BigDecimal.valueOf(forecastMonths)));
        }

        // Monthly forecasts
        List<ExpenseForecastResponse.MonthlyForecast> monthlyForecasts = new ArrayList<>();
        for (int i = 1; i <= forecastMonths; i++) {
            LocalDate forecastMonth = now.plusMonths(i).withDayOfMonth(1);
            BigDecimal monthTotal = categoryForecasts.stream()
                    .map(ExpenseForecastResponse.CategoryForecast::getForecastedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthlyForecasts.add(ExpenseForecastResponse.MonthlyForecast.builder()
                    .month(forecastMonth.getMonth().name() + " " + forecastMonth.getYear())
                    .projectedExpenses(monthTotal)
                    .confidence(BigDecimal.valueOf(Math.max(60, 95 - (i * 10))))
                    .trend(i == 1 ? "HIGH_CONFIDENCE" : i == 2 ? "MEDIUM_CONFIDENCE" : "LOW_CONFIDENCE")
                    .build());
        }

        return ExpenseForecastResponse.builder()
                .forecastMonths(forecastMonths)
                .totalForecastedExpenses(totalForecasted)
                .monthlyForecasts(monthlyForecasts)
                .categoryForecasts(categoryForecasts)
                .methodology("Weighted Moving Average (6 months) with seasonal trend detection")
                .build();
    }

    // ==============================
    // Feature 3: What-If Budget Simulator
    // ==============================
    @Override
    public WhatIfSimulationResponse simulateWhatIf(String userId, WhatIfSimulationRequest request) {
        LocalDate now = LocalDate.now();
        LocalDate threeMonthsAgo = now.minusMonths(3);

        // --- Données réelles brutes depuis accounting_entry ---
        BigDecimal totalIncome = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.INCOME, threeMonthsAgo, now);
        BigDecimal totalExpense = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, threeMonthsAgo, now);
        Long incomeEntries = entryRepository.countByUserIdAndTypeAndDateRange(userId, EntryType.INCOME, threeMonthsAgo, now);
        Long expenseEntries = entryRepository.countByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, threeMonthsAgo, now);

        BigDecimal monthlyIncome = totalIncome.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyExpenses = totalExpense.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal currentNet = monthlyIncome.subtract(monthlyExpenses);

        String dataSource = String.format(
                "Basé sur %d écritures REVENUS et %d écritures DÉPENSES réelles entre le %s et le %s (moyenne sur 3 mois)",
                incomeEntries, expenseEntries, threeMonthsAgo, now);

        // Catégories essentielles agricoles — réductions agressives = risque réel
        Set<EntryCategory> essentialCategories = Set.of(
                EntryCategory.SEEDS, EntryCategory.FERTILIZER, EntryCategory.IRRIGATION);

        List<String> feasibilityWarnings = new ArrayList<>();

        // Vérifier la faisabilité de la hausse de revenus demandée
        if (request.getIncomeChangePercent() != null) {
            double incChange = request.getIncomeChangePercent();
            if (incChange > 50)
                feasibilityWarnings.add(String.format(
                        "Hausse des revenus de +%.0f%% : très optimiste — aucune base historique ne justifie une telle augmentation.", incChange));
            else if (incChange > 25)
                feasibilityWarnings.add(String.format(
                        "Hausse des revenus de +%.0f%% : ambitieux — à valider avec des données concrètes (nouveaux marchés, cultures).", incChange));
        }

        // Apply income change
        BigDecimal simulatedIncome = monthlyIncome;
        if (request.getIncomeChangePercent() != null) {
            simulatedIncome = monthlyIncome.multiply(
                    BigDecimal.ONE.add(BigDecimal.valueOf(request.getIncomeChangePercent() / 100)));
        }

        // Apply category changes — chaque catégorie basée sur les vraies écritures
        Map<EntryCategory, WhatIfSimulationResponse.CategoryImpact> categoryImpacts = new LinkedHashMap<>();
        BigDecimal simulatedExpenses = BigDecimal.ZERO;

        for (EntryCategory category : EntryCategory.values()) {
            BigDecimal catTotal = entryRepository.sumAmountByCategoryAndDateRange(
                    userId, EntryType.EXPENSE, category, threeMonthsAgo, now);
            BigDecimal catMonthly = catTotal.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

            int catEntries = entryRepository.findByUserIdAndEntryTypeAndCategoryAndEntryDateBetween(
                    userId, EntryType.EXPENSE, category, threeMonthsAgo, now).size();

            double changePct = 0;
            if (request.getCategoryChanges() != null && request.getCategoryChanges().containsKey(category)) {
                changePct = request.getCategoryChanges().get(category);
            }

            BigDecimal simulated = catMonthly.multiply(
                    BigDecimal.ONE.add(BigDecimal.valueOf(changePct / 100))).setScale(2, RoundingMode.HALF_UP);

            // --- Évaluation de la faisabilité par catégorie ---
            String feasibility = "REALISTIC";
            if (changePct != 0) {
                if (catMonthly.compareTo(BigDecimal.ZERO) == 0) {
                    feasibility = "NO_DATA";
                    feasibilityWarnings.add(String.format(
                            "%s : aucune dépense historique dans cette catégorie — simulation sans base réelle.", category));
                } else if (changePct >= 100) {
                    feasibility = "UNREALISTIC";
                    feasibilityWarnings.add(String.format(
                            "%s : hausse de +%.0f%% irréaliste — aucune base concrète ne justifie un doublement ou plus de ces dépenses.", category, changePct));
                } else if (changePct >= 50) {
                    feasibility = "AGGRESSIVE";
                    feasibilityWarnings.add(String.format(
                            "%s : hausse de +%.0f%% très agressive — à justifier par un investissement ou une expansion concrète.", category, changePct));
                } else if (changePct <= -50) {
                    feasibility = "UNREALISTIC";
                    feasibilityWarnings.add(String.format(
                            "%s : réduction de %.0f%% irréaliste — il est impossible de supprimer plus de la moitié de ces dépenses.", category, Math.abs(changePct)));
                } else if (changePct <= -30 && essentialCategories.contains(category)) {
                    feasibility = "AGGRESSIVE";
                    feasibilityWarnings.add(String.format(
                            "%s est une dépense essentielle agricole — une réduction de %.0f%% risque d'impacter directement la production.", category, Math.abs(changePct)));
                } else if (changePct <= -25) {
                    feasibility = "AGGRESSIVE";
                    feasibilityWarnings.add(String.format(
                            "%s : réduction de %.0f%% agressive — difficile à tenir sur plusieurs mois.", category, Math.abs(changePct)));
                }
            }

            if (catMonthly.compareTo(BigDecimal.ZERO) > 0 || changePct != 0) {
                String formula;
                if (changePct == 0) {
                    formula = String.format("%.2f TND/mois (inchangé — total réel %d écritures: %.2f TND / 3 mois)",
                            catMonthly.doubleValue(), catEntries, catTotal.doubleValue());
                } else {
                    formula = String.format("%.2f × (1 %s %.0f%%) = %.2f TND/mois (total réel %d écritures: %.2f TND / 3 mois)",
                            catMonthly.doubleValue(), changePct > 0 ? "+" : "", changePct,
                            simulated.doubleValue(), catEntries, catTotal.doubleValue());
                }

                categoryImpacts.put(category, WhatIfSimulationResponse.CategoryImpact.builder()
                        .realTotalLast3Months(catTotal)
                        .numberOfEntries(catEntries)
                        .currentAmount(catMonthly)
                        .simulatedAmount(simulated)
                        .changePercent(changePct)
                        .formula(formula)
                        .feasibility(feasibility)
                        .build());
            }

            simulatedExpenses = simulatedExpenses.add(simulated);
        }

        BigDecimal simulatedNet = simulatedIncome.subtract(simulatedExpenses);
        BigDecimal netImpact = simulatedNet.subtract(currentNet);

        String verdict;
        if (netImpact.compareTo(BigDecimal.ZERO) > 0) verdict = "POSITIVE";
        else if (netImpact.compareTo(BigDecimal.ZERO) < 0) verdict = "NEGATIVE";
        else verdict = "NEUTRAL";

        // Monthly projections
        int simMonths = request.getSimulationMonths() != null ? request.getSimulationMonths() : 3;
        List<WhatIfSimulationResponse.MonthProjection> projections = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;

        for (int i = 1; i <= simMonths; i++) {
            cumulative = cumulative.add(simulatedNet);
            LocalDate month = now.plusMonths(i).withDayOfMonth(1);
            projections.add(WhatIfSimulationResponse.MonthProjection.builder()
                    .month(month.getMonth().name() + " " + month.getYear())
                    .projectedIncome(simulatedIncome)
                    .projectedExpenses(simulatedExpenses)
                    .projectedNet(simulatedNet)
                    .cumulativeSavings(cumulative)
                    .build());
        }

        return WhatIfSimulationResponse.builder()
                .analysisPeriodFrom(threeMonthsAgo)
                .analysisPeriodTo(now)
                .totalIncomeEntriesAnalyzed(incomeEntries)
                .totalExpenseEntriesAnalyzed(expenseEntries)
                .dataSource(dataSource)
                .currentMonthlyIncome(monthlyIncome)
                .currentMonthlyExpenses(monthlyExpenses)
                .currentNetIncome(currentNet)
                .simulatedMonthlyIncome(simulatedIncome)
                .simulatedMonthlyExpenses(simulatedExpenses)
                .simulatedNetIncome(simulatedNet)
                .netImpact(netImpact)
                .verdict(verdict)
                .feasibilityWarnings(feasibilityWarnings)
                .categoryImpacts(categoryImpacts)
                .monthlyProjections(projections)
                .build();
    }

    // ==============================
    // Feature 4: Profitability Trend Analysis
    // ==============================
    @Override
    public ProfitabilityTrendResponse getProfitabilityTrend(String userId, Integer months) {
        int numMonths = months != null ? months : 6;
        LocalDate now = LocalDate.now();

        List<ProfitabilityTrendResponse.MonthlyProfit> monthlyProfits = new ArrayList<>();
        String bestMonth = null;
        BigDecimal bestProfit = null;
        String worstMonth = null;
        BigDecimal worstProfit = null;
        BigDecimal totalProfit = BigDecimal.ZERO;

        int totalPoints = numMonths + 1; // include current month
        double[] xValues = new double[totalPoints];
        double[] yValues = new double[totalPoints];

        for (int i = numMonths; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = (i == 0) ? now : monthStart.plusMonths(1).minusDays(1); // current month uses today
            String monthLabel = monthStart.getMonth().name() + " " + monthStart.getYear();

            BigDecimal income = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.INCOME, monthStart, monthEnd);
            BigDecimal expense = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, monthStart, monthEnd);
            BigDecimal net = income.subtract(expense);

            BigDecimal profitMargin = income.compareTo(BigDecimal.ZERO) > 0
                    ? net.divide(income, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            monthlyProfits.add(ProfitabilityTrendResponse.MonthlyProfit.builder()
                    .month(monthLabel).income(income).expenses(expense)
                    .netProfit(net).profitMargin(profitMargin).build());

            totalProfit = totalProfit.add(net);

            if (bestProfit == null || net.compareTo(bestProfit) > 0) {
                bestProfit = net; bestMonth = monthLabel;
            }
            if (worstProfit == null || net.compareTo(worstProfit) < 0) {
                worstProfit = net; worstMonth = monthLabel;
            }

            int idx = numMonths - i;
            xValues[idx] = idx;
            yValues[idx] = net.doubleValue();
        }

        BigDecimal avgProfit = totalProfit.divide(BigDecimal.valueOf(totalPoints), 2, RoundingMode.HALF_UP);

        // Linear regression
        ProfitabilityTrendResponse.LinearRegressionResult regression = computeLinearRegression(xValues, yValues);

        // Overall growth rate
        BigDecimal overallGrowthRate = BigDecimal.ZERO;
        if (monthlyProfits.size() >= 2) {
            BigDecimal first = monthlyProfits.get(0).getNetProfit();
            BigDecimal last = monthlyProfits.get(monthlyProfits.size() - 1).getNetProfit();
            if (first.compareTo(BigDecimal.ZERO) != 0) {
                overallGrowthRate = last.subtract(first).divide(first.abs(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }

        String trend = regression.getSlope() > 50 ? "GROWING" : regression.getSlope() < -50 ? "DECLINING" : "STABLE";

        return ProfitabilityTrendResponse.builder()
                .monthlyProfits(monthlyProfits)
                .overallGrowthRate(overallGrowthRate)
                .overallTrend(trend)
                .bestMonth(bestMonth).bestMonthProfit(bestProfit)
                .worstMonth(worstMonth).worstMonthProfit(worstProfit)
                .averageMonthlyProfit(avgProfit)
                .regression(regression)
                .build();
    }

    private ProfitabilityTrendResponse.LinearRegressionResult computeLinearRegression(double[] x, double[] y) {
        int n = x.length;
        if (n < 2) {
            return ProfitabilityTrendResponse.LinearRegressionResult.builder()
                    .slope(0).intercept(0).rSquared(0).interpretation("Pas assez de données").build();
        }

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i]; sumY += y[i]; sumXY += x[i] * y[i]; sumX2 += x[i] * x[i];
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) {
            return ProfitabilityTrendResponse.LinearRegressionResult.builder()
                    .slope(0).intercept(sumY / n).rSquared(0).interpretation("Données constantes").build();
        }

        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;

        // R-squared
        double meanY = sumY / n;
        double ssTotal = 0, ssResidual = 0;
        for (int i = 0; i < n; i++) {
            double predicted = slope * x[i] + intercept;
            ssResidual += (y[i] - predicted) * (y[i] - predicted);
            ssTotal += (y[i] - meanY) * (y[i] - meanY);
        }
        double rSquared = ssTotal > 0 ? 1 - (ssResidual / ssTotal) : 0;

        String interpretation;
        String reliabilityNote = rSquared > 0.6 ? "fiabilité bonne" : rSquared > 0.3 ? "fiabilité modérée — données irrégulières" : "fiabilité faible — peu de données disponibles";
        if (slope > 100)
            interpretation = String.format(
                    "Progression solide : la rentabilité gagne +%.0f TND/mois en tendance (R²=%.2f, %s). " +
                    "Votre exploitation s'améliore régulièrement — maintenez cette dynamique.",
                    Math.abs(slope), rSquared, reliabilityNote);
        else if (slope > 0)
            interpretation = String.format(
                    "Légère hausse : +%.0f TND/mois de progression (R²=%.2f, %s). " +
                    "Tendance positive mais fragile — maîtrisez vos dépenses variables pour la consolider.",
                    Math.abs(slope), rSquared, reliabilityNote);
        else if (slope > -100)
            interpretation = String.format(
                    "Légère baisse : %.0f TND/mois de moins en tendance (R²=%.2f, %s). " +
                    "Vérifiez les catégories dont les dépenses ont augmenté ces derniers mois pour inverser la tendance.",
                    Math.abs(slope), rSquared, reliabilityNote);
        else
            interpretation = String.format(
                    "Baisse sévère : %.0f TND/mois de moins chaque mois (R²=%.2f, %s). " +
                    "Action corrective urgente — analysez vos 3 catégories de dépenses les plus élevées et réduisez-les immédiatement.",
                    Math.abs(slope), rSquared, reliabilityNote);

        return ProfitabilityTrendResponse.LinearRegressionResult.builder()
                .slope(Math.round(slope * 100.0) / 100.0)
                .intercept(Math.round(intercept * 100.0) / 100.0)
                .rSquared(Math.round(rSquared * 10000.0) / 10000.0)
                .interpretation(interpretation)
                .build();
    }

    // ==============================
    // Feature 5: Smart Categorization
    // ==============================
    private static final Map<EntryCategory, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    static {
        CATEGORY_KEYWORDS.put(EntryCategory.SEEDS, Arrays.asList("semence", "seed", "graine", "plantation", "plant", "semis", "bouture"));
        CATEGORY_KEYWORDS.put(EntryCategory.FERTILIZER, Arrays.asList("engrais", "fertilizer", "compost", "npk", "phosphate", "azote", "amendement", "fumier"));
        CATEGORY_KEYWORDS.put(EntryCategory.IRRIGATION, Arrays.asList("irrigation", "eau", "water", "pompe", "pump", "tuyau", "arrosage", "goutte", "aspersion"));
        CATEGORY_KEYWORDS.put(EntryCategory.LABOR, Arrays.asList("main d'oeuvre", "labor", "ouvrier", "salaire", "wage", "travailleur", "journalier", "employe"));
        CATEGORY_KEYWORDS.put(EntryCategory.TRANSPORT, Arrays.asList("transport", "camion", "livraison", "carburant", "fuel", "gasoil", "essence", "vehicule", "delivery"));
        CATEGORY_KEYWORDS.put(EntryCategory.EQUIPMENT, Arrays.asList("equipement", "equipment", "machine", "tracteur", "tractor", "outil", "tool", "materiel", "reparation", "maintenance"));
        CATEGORY_KEYWORDS.put(EntryCategory.INSURANCE, Arrays.asList("assurance", "insurance", "police", "prime", "couverture", "sinistre", "risque"));
        CATEGORY_KEYWORDS.put(EntryCategory.LOAN_PAYMENT, Arrays.asList("pret", "loan", "credit", "remboursement", "echeance", "interet", "banque", "dette"));
        CATEGORY_KEYWORDS.put(EntryCategory.SALES, Arrays.asList("vente", "sale", "revenu", "recolte", "harvest", "marche", "client", "facture", "produit", "cereale"));
        CATEGORY_KEYWORDS.put(EntryCategory.OTHER, Arrays.asList("divers", "other", "autre", "frais", "misc"));
    }

    @Override
    public SmartCategorizationResponse categorize(String description) {
        String normalized = description.toLowerCase().trim();
        List<SmartCategorizationResponse.CategoryScore> allScores = new ArrayList<>();
        EntryCategory bestCategory = EntryCategory.OTHER;
        double bestScore = 0;

        for (Map.Entry<EntryCategory, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            EntryCategory category = entry.getKey();
            List<String> keywords = entry.getValue();
            List<String> matched = new ArrayList<>();
            double score = 0;

            for (String keyword : keywords) {
                if (normalized.contains(keyword.toLowerCase())) {
                    matched.add(keyword);
                    score += keyword.length() > 4 ? 2.0 : 1.0; // longer keywords = higher weight
                }
            }

            // Normalize score
            double normalizedScore = keywords.isEmpty() ? 0 : (score / (keywords.size() * 2.0)) * 100;
            normalizedScore = Math.min(100, normalizedScore);

            allScores.add(SmartCategorizationResponse.CategoryScore.builder()
                    .category(category).score(Math.round(normalizedScore * 100.0) / 100.0)
                    .matchedKeywords(matched).build());

            if (normalizedScore > bestScore) {
                bestScore = normalizedScore;
                bestCategory = category;
            }
        }

        allScores.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        return SmartCategorizationResponse.builder()
                .inputDescription(description)
                .suggestedCategory(bestCategory)
                .confidenceScore(Math.round(bestScore * 100.0) / 100.0)
                .allScores(allScores)
                .build();
    }

    // ==============================
    // Feature 6: Predictive Budget Alerts
    // ==============================
    @Override
    public PredictiveBudgetAlertResponse getPredictiveBudgetAlerts(String userId) {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);

        List<Budget> currentBudgets = budgetRepository.findActiveBudgets(userId, now, null);
        List<PredictiveBudgetAlertResponse.PredictiveAlert> alerts = new ArrayList<>();

        int daysPassed = (int) ChronoUnit.DAYS.between(monthStart, now) + 1;
        int daysInMonth = now.lengthOfMonth();
        int daysRemaining = daysInMonth - daysPassed;

        for (Budget budget : currentBudgets) {
            BigDecimal spentSoFar = entryRepository.sumAmountByCategoryAndDateRange(
                    userId, EntryType.EXPENSE, budget.getCategory(),
                    budget.getPeriodStart(), now);

            if (daysPassed <= 0) continue;

            BigDecimal dailyBurnRate = spentSoFar.divide(BigDecimal.valueOf(daysPassed), 2, RoundingMode.HALF_UP);
            BigDecimal projectedMonthEnd = spentSoFar.add(dailyBurnRate.multiply(BigDecimal.valueOf(daysRemaining)));
            BigDecimal projectedOverspend = projectedMonthEnd.subtract(budget.getPlannedAmount());

            if (projectedOverspend.compareTo(BigDecimal.ZERO) > 0) {
                // Estimate exhaustion date
                BigDecimal remainingBudget = budget.getPlannedAmount().subtract(spentSoFar);
                int daysUntilExhausted = dailyBurnRate.compareTo(BigDecimal.ZERO) > 0
                        ? remainingBudget.divide(dailyBurnRate, 0, RoundingMode.CEILING).intValue()
                        : daysRemaining;
                LocalDate exhaustionDate = now.plusDays(Math.max(0, daysUntilExhausted));

                double overPct = budget.getPlannedAmount().compareTo(BigDecimal.ZERO) > 0
                        ? projectedOverspend.divide(budget.getPlannedAmount(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                        : 0;

                String severity;
                if (spentSoFar.compareTo(budget.getPlannedAmount()) > 0) severity = "CRITICAL";
                else if (overPct > 30) severity = "HIGH";
                else if (overPct > 10) severity = "MEDIUM";
                else severity = "LOW";

                String recommendation;
                if ("CRITICAL".equals(severity)) {
                    BigDecimal overAmount = spentSoFar.subtract(budget.getPlannedAmount());
                    BigDecimal dailyCutNeeded = daysRemaining > 0
                            ? overAmount.divide(BigDecimal.valueOf(daysRemaining), 1, RoundingMode.CEILING) : overAmount;
                    recommendation = String.format(
                            "Budget DÉPASSÉ en %s : %.0f TND dépensés sur %.0f TND prévus (dépassement de %.0f TND). " +
                            "Il reste %d jours — réduisez de %.1f TND/jour minimum pour limiter les dégâts. Stoppez tout achat non urgent dans cette catégorie.",
                            budget.getCategory(), spentSoFar.doubleValue(), budget.getPlannedAmount().doubleValue(),
                            overAmount.doubleValue(), daysRemaining, dailyCutNeeded.doubleValue());
                } else if ("HIGH".equals(severity)) {
                    BigDecimal allowedDaily = budget.getPlannedAmount().subtract(spentSoFar)
                            .divide(BigDecimal.valueOf(Math.max(1, daysRemaining)), 1, RoundingMode.HALF_UP);
                    recommendation = String.format(
                            "Dépassement prévu de %.0f%% en %s : %.0f TND dépensés, %.0f TND projetés sur un budget de %.0f TND. " +
                            "Limitez-vous à %.1f TND/jour (vs %.1f TND/jour actuellement) pour tenir le budget sur les %d jours restants.",
                            overPct, budget.getCategory(), spentSoFar.doubleValue(), projectedMonthEnd.doubleValue(),
                            budget.getPlannedAmount().doubleValue(), allowedDaily.doubleValue(), dailyBurnRate.doubleValue(), daysRemaining);
                } else {
                    recommendation = String.format(
                            "%s : %.0f TND dépensés à ce jour sur %.0f TND prévus — rythme de %.1f TND/jour légèrement élevé. " +
                            "Projeté à %.0f TND en fin de mois (+%.0f TND au-dessus du budget). " +
                            "Vérifiez si les prochains achats dans cette catégorie sont vraiment nécessaires ce mois-ci.",
                            budget.getCategory(), spentSoFar.doubleValue(), budget.getPlannedAmount().doubleValue(),
                            dailyBurnRate.doubleValue(), projectedMonthEnd.doubleValue(), projectedOverspend.doubleValue());
                }

                alerts.add(PredictiveBudgetAlertResponse.PredictiveAlert.builder()
                        .category(budget.getCategory())
                        .budgetedAmount(budget.getPlannedAmount())
                        .spentSoFar(spentSoFar)
                        .dailyBurnRate(dailyBurnRate)
                        .projectedMonthEnd(projectedMonthEnd)
                        .projectedOverspend(projectedOverspend)
                        .daysRemaining(daysRemaining)
                        .estimatedExhaustionDate(exhaustionDate)
                        .severity(severity)
                        .recommendation(recommendation)
                        .build());
            }
        }

        alerts.sort((a, b) -> {
            Map<String, Integer> order = Map.of("CRITICAL", 0, "HIGH", 1, "MEDIUM", 2, "LOW", 3);
            return Integer.compare(order.getOrDefault(a.getSeverity(), 4), order.getOrDefault(b.getSeverity(), 4));
        });

        return PredictiveBudgetAlertResponse.builder()
                .alerts(alerts)
                .totalAlerts(alerts.size())
                .build();
    }
}