package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.dto_savings_accountability.*;
import tn.esprit.agri.entities.SavingsAccount;
import tn.esprit.agri.entities.SavingsGoal;
import tn.esprit.agri.entities.enums.EntryType;
import tn.esprit.agri.entities.enums.SavingsTransactionType;
import tn.esprit.agri.exception.ResourceNotFoundException;
import tn.esprit.agri.repositories.AccountingEntryRepository;
import tn.esprit.agri.repositories.SavingsAccountRepository;
import tn.esprit.agri.repositories.SavingsGoalRepository;
import tn.esprit.agri.repositories.SavingsTransactionRepository;
import tn.esprit.agri.services.ISavingsAIService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavingsAIServiceImpl implements ISavingsAIService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsTransactionRepository transactionRepository;
    private final AccountingEntryRepository accountingEntryRepository;
    private final SavingsGoalRepository goalRepository;

    // ======================================================================
    // Feature 7 : Prédiction d'atteinte des objectifs (par objectif)
    // ======================================================================

    @Override
    public GoalAchievementPredictionResponse predictGoalAchievement(String userId) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        BigDecimal currentBalance = account.getCurrentBalance();

        // --- Weighted moving average of net savings (last 7 months, recent months weigh more) ---
        LocalDateTime now = LocalDateTime.now();
        double[] weights = {1.0, 1.0, 1.5, 1.5, 2.0, 2.0, 2.5}; // index 0 = 6 months ago, 6 = current
        BigDecimal totalWeighted = BigDecimal.ZERO;
        double totalWeight = 0;
        int monthsWithActivity = 0;

        for (int i = 6; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i)
                    .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime monthEnd = i == 0 ? now : monthStart.plusMonths(1).minusSeconds(1);

            BigDecimal deposits = transactionRepository.sumAmountByAccountIdAndTypeAndDateRange(
                    account.getId(), SavingsTransactionType.DEPOSIT, monthStart, monthEnd);
            BigDecimal withdrawals = transactionRepository.sumAmountByAccountIdAndTypeAndDateRange(
                    account.getId(), SavingsTransactionType.WITHDRAWAL, monthStart, monthEnd);
            BigDecimal net = deposits.subtract(withdrawals);

            int idx = 6 - i;
            if (deposits.compareTo(BigDecimal.ZERO) > 0 || withdrawals.compareTo(BigDecimal.ZERO) > 0)
                monthsWithActivity++;

            totalWeighted = totalWeighted.add(net.multiply(BigDecimal.valueOf(weights[idx])));
            totalWeight += weights[idx];
        }

        BigDecimal avgMonthlySavings = totalWeight > 0
                ? totalWeighted.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String dataExplanation = monthsWithActivity == 0
                ? "Aucune transaction d'épargne trouvée sur les 7 derniers mois."
                : "Moyenne pondérée basée sur " + monthsWithActivity + " mois actif(s) "
                + "sur les 7 derniers mois (les mois récents ont plus de poids : 2.5x pour le mois courant).";

        // --- Fetch all active (non-collected) goals ---
        List<SavingsGoal> activeGoals = goalRepository
                .findBySavingsAccountIdOrderByTargetDateAsc(account.getId())
                .stream().filter(g -> !g.getCollected()).toList();

        // --- Separate into 3 allocation groups ---
        // Group 1: Fixed-percentage goals (CUSTOM_PERCENTAGE) — independent of each other
        List<SavingsGoal> customGoals = activeGoals.stream()
                .filter(g -> g.getCustomAllocationPercentage() != null
                        && g.getCustomAllocationPercentage().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        // Group 2: Priority goals (AUTO_PRIORITY) — funded sequentially: P1 fully before P2
        List<SavingsGoal> priorityGoals = activeGoals.stream()
                .filter(g -> (g.getCustomAllocationPercentage() == null
                        || g.getCustomAllocationPercentage().compareTo(BigDecimal.ZERO) == 0)
                        && g.getPriority() > 0)
                .sorted(java.util.Comparator.comparingInt(SavingsGoal::getPriority))
                .toList();

        // Group 3: Proportional goals (AUTO_PROPORTIONAL) — share residual budget in parallel
        List<SavingsGoal> proportionalGoals = activeGoals.stream()
                .filter(g -> (g.getCustomAllocationPercentage() == null
                        || g.getCustomAllocationPercentage().compareTo(BigDecimal.ZERO) == 0)
                        && g.getPriority() == 0)
                .toList();

        // --- Compute budget allocations ---
        BigDecimal totalFixedPct = customGoals.stream()
                .map(SavingsGoal::getCustomAllocationPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal residualPct = BigDecimal.valueOf(100).subtract(totalFixedPct).max(BigDecimal.ZERO);
        BigDecimal residualAvgSavings = avgMonthlySavings
                .multiply(residualPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalProportionalTarget = proportionalGoals.stream()
                .filter(g -> !g.getAchieved())
                .map(SavingsGoal::getTargetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Pre-compute total months consumed by ALL priority goals (used as offset for proportional goals)
        int totalPriorityMonths = 0;
        for (SavingsGoal g : priorityGoals) {
            if (!g.getAchieved() && residualAvgSavings.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal rem = g.getTargetAmount().subtract(g.getCurrentAmount()).max(BigDecimal.ZERO);
                if (rem.compareTo(BigDecimal.ZERO) > 0)
                    totalPriorityMonths += rem.divide(residualAvgSavings, 0, RoundingMode.CEILING).intValue();
            }
        }

        // --- Build predictions in priority order: CUSTOM → PRIORITY → PROPORTIONAL ---
        List<GoalAchievementPredictionResponse.GoalPrediction> goalPredictions = new ArrayList<>();

        // 1. CUSTOM_PERCENTAGE goals — run independently, no waiting offset
        for (SavingsGoal goal : customGoals) {
            BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
            BigDecimal monthlyContrib = avgMonthlySavings
                    .multiply(goal.getCustomAllocationPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            goalPredictions.add(buildGoalPrediction(goal, "CUSTOM_PERCENTAGE", monthlyContrib, remaining, 0));
        }

        // 2. AUTO_PRIORITY goals — sequential: P1 must complete before P2 starts, etc.
        int runningPriorityOffset = 0;
        for (SavingsGoal goal : priorityGoals) {
            BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
            goalPredictions.add(buildGoalPrediction(goal, "AUTO_PRIORITY", residualAvgSavings, remaining, runningPriorityOffset));
            // Accumulate months for next priority goal's start offset
            if (!goal.getAchieved() && residualAvgSavings.compareTo(BigDecimal.ZERO) > 0
                    && remaining.compareTo(BigDecimal.ZERO) > 0)
                runningPriorityOffset += remaining.divide(residualAvgSavings, 0, RoundingMode.CEILING).intValue();
        }

        // 3. AUTO_PROPORTIONAL goals — share residual in parallel, but start after all priority goals
        for (SavingsGoal goal : proportionalGoals) {
            BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
            BigDecimal monthlyContrib = totalProportionalTarget.compareTo(BigDecimal.ZERO) > 0
                    ? residualAvgSavings.multiply(goal.getTargetAmount())
                    .divide(totalProportionalTarget, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            goalPredictions.add(buildGoalPrediction(goal, "AUTO_PROPORTIONAL", monthlyContrib, remaining, totalPriorityMonths));
        }

        // --- Global scenarios (based on total remaining across all active goals) ---
        BigDecimal totalRemaining = activeGoals.stream()
                .filter(g -> !g.getAchieved())
                .map(g -> g.getTargetAmount().subtract(g.getCurrentAmount()).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<GoalAchievementPredictionResponse.Scenario> scenarios = new ArrayList<>();
        if (avgMonthlySavings.compareTo(BigDecimal.ZERO) > 0 && totalRemaining.compareTo(BigDecimal.ZERO) > 0) {
            int currentMonths = totalRemaining.divide(avgMonthlySavings, 0, RoundingMode.CEILING).intValue();
            scenarios.add(buildScenario("Rythme actuel", avgMonthlySavings, currentMonths,
                    "En maintenant votre rythme d'épargne actuel (" + avgMonthlySavings + " DT/mois)"));

            BigDecimal aggressive = avgMonthlySavings.multiply(BigDecimal.valueOf(1.5)).setScale(2, RoundingMode.HALF_UP);
            int aggMonths = totalRemaining.divide(aggressive, 0, RoundingMode.CEILING).intValue();
            scenarios.add(buildScenario("Ambitieux (+50%)", aggressive, aggMonths,
                    "En augmentant vos dépôts de 50% — soit " + aggressive + " DT/mois"));

            BigDecimal conservative = avgMonthlySavings.multiply(BigDecimal.valueOf(0.75)).setScale(2, RoundingMode.HALF_UP);
            if (conservative.compareTo(BigDecimal.ZERO) > 0) {
                int consMonths = totalRemaining.divide(conservative, 0, RoundingMode.CEILING).intValue();
                scenarios.add(buildScenario("Prudent (-25%)", conservative, consMonths,
                        "Si vos dépôts diminuent de 25% — soit " + conservative + " DT/mois"));
            }

            BigDecimal target12 = totalRemaining.divide(BigDecimal.valueOf(12), 2, RoundingMode.CEILING);
            scenarios.add(buildScenario("Objectif 12 mois", target12, 12,
                    "Montant à épargner chaque mois pour financer tous les objectifs en 1 an"));
        }

        return GoalAchievementPredictionResponse.builder()
                .currentBalance(currentBalance)
                .averageMonthlySavings(avgMonthlySavings)
                .monthsAnalyzed(monthsWithActivity)
                .dataExplanation(dataExplanation)
                .goalPredictions(goalPredictions)
                .scenarios(scenarios)
                .build();
    }

    private GoalAchievementPredictionResponse.GoalPrediction buildGoalPrediction(
            SavingsGoal goal, String allocationMode, BigDecimal monthlyContrib,
            BigDecimal remaining, int monthsOffset) {

        Integer estimatedMonths = null;
        LocalDate estimatedDate = null;
        double probability = 0;
        String status;
        String statusDetail;

        if (goal.getAchieved()) {
            status = "ACHIEVED";
            statusDetail = String.format(
                    "Objectif atteint ! %.0f TND sur %.0f TND — vous pouvez le collecter dès maintenant.",
                    goal.getCurrentAmount().doubleValue(), goal.getTargetAmount().doubleValue());
            probability = 100;
        } else if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            status = "ACHIEVED";
            statusDetail = String.format(
                    "Montant cible atteint : %.0f TND. Vous pouvez collecter cet objectif.",
                    goal.getTargetAmount().doubleValue());
            probability = 100;
        } else if (monthlyContrib.compareTo(BigDecimal.ZERO) <= 0) {
            status = "BLOCKED";
            statusDetail = String.format(
                    "Aucune épargne allouée à cet objectif (%.0f TND restants sur %.0f TND). " +
                            "Faites un dépôt ou définissez une priorité/pourcentage pour débloquer la progression.",
                    remaining.doubleValue(), goal.getTargetAmount().doubleValue());
            probability = 0;
        } else {
            int rawMonths = remaining.divide(monthlyContrib, 0, RoundingMode.CEILING).intValue();
            estimatedMonths = rawMonths + monthsOffset;
            estimatedDate = LocalDate.now().plusMonths(estimatedMonths);

            boolean beforeTargetDate = goal.getTargetDate() == null || !estimatedDate.isAfter(goal.getTargetDate());
            probability = Math.min(95, 50.0 + (monthlyContrib.doubleValue() / remaining.doubleValue()) * 200);

            String waitNote = monthsOffset > 0
                    ? String.format(" (dont %d mois d'attente pendant que les objectifs prioritaires sont financés)", monthsOffset)
                    : "";

            if (beforeTargetDate) {
                status = "ON_TRACK";
                statusDetail = String.format(
                        "En bonne voie : %.0f TND restants à %.0f TND/mois alloués = %d mois%s. " +
                                "Completion prévue le %s%s.",
                        remaining.doubleValue(), monthlyContrib.doubleValue(), estimatedMonths, waitNote,
                        estimatedDate,
                        goal.getTargetDate() != null ? " — avant la date cible du " + goal.getTargetDate() : "");
            } else {
                status = "AT_RISK";
                long delayMonths = goal.getTargetDate() != null
                        ? java.time.temporal.ChronoUnit.MONTHS.between(goal.getTargetDate(), estimatedDate) : 0;
                statusDetail = String.format(
                        "Retard probable : %.0f TND restants à %.0f TND/mois = %d mois%s. " +
                                "Completion prévue le %s — soit %d mois après la date cible du %s. " +
                                "Augmentez vos dépôts ou ajustez la date cible.",
                        remaining.doubleValue(), monthlyContrib.doubleValue(), estimatedMonths, waitNote,
                        estimatedDate, delayMonths, goal.getTargetDate());
                probability = Math.min(probability, 60);
            }
        }

        double progress = goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                ? goal.getCurrentAmount().divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0;

        return GoalAchievementPredictionResponse.GoalPrediction.builder()
                .goalId(goal.getId())
                .goalName(goal.getGoalName())
                .priority(goal.getPriority())
                .allocationMode(allocationMode)
                .customAllocationPct(goal.getCustomAllocationPercentage())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .remaining(remaining)
                .progressPercentage(Math.round(progress * 100.0) / 100.0)
                .targetDate(goal.getTargetDate())
                .estimatedMonthlyContribution(monthlyContrib)
                .estimatedMonthsToComplete(estimatedMonths)
                .estimatedCompletionDate(estimatedDate)
                .probability(Math.round(probability * 100.0) / 100.0)
                .status(status)
                .statusDetail(statusDetail)
                .build();
    }

    private GoalAchievementPredictionResponse.Scenario buildScenario(
            String name, BigDecimal monthly, int months, String desc) {
        return GoalAchievementPredictionResponse.Scenario.builder()
                .name(name).monthlySavings(monthly)
                .achievementDate(LocalDate.now().plusMonths(months))
                .monthsRequired(months).description(desc).build();
    }

    // ======================================================================
    // Feature 8 : Plan d'épargne intelligent (saisonnier)
    // ======================================================================

    @Override
    public SmartSavingsPlanResponse getSmartSavingsPlan(String userId) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        BigDecimal goalAmount = account.getGoalAmount() != null ? account.getGoalAmount() : BigDecimal.ZERO;
        BigDecimal currentBalance = account.getCurrentBalance();
        BigDecimal remaining = goalAmount.subtract(currentBalance).max(BigDecimal.ZERO);
        LocalDate now = LocalDate.now();
        LocalDate historicalFrom = now.minusMonths(12).withDayOfMonth(1);

        // --- Facteurs saisonniers : mois calendaire réel → valeur nette historique ---
        // Clé = numéro du mois (1=Jan…12=Déc), Valeur = revenu net de CE mois l'année passée
        java.util.Map<Integer, Double> netByCalendarMonth = new java.util.HashMap<>();
        BigDecimal totalDisposable = BigDecimal.ZERO;
        int monthsWithData = 0;

        for (int i = 0; i < 12; i++) {
            LocalDate monthStart = now.minusMonths(12 - i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            BigDecimal income = accountingEntryRepository.sumAmountByUserIdAndTypeAndDateRange(
                    userId, EntryType.INCOME, monthStart, monthEnd);
            BigDecimal expense = accountingEntryRepository.sumAmountByUserIdAndTypeAndDateRange(
                    userId, EntryType.EXPENSE, monthStart, monthEnd);
            BigDecimal net = income.subtract(expense);
            totalDisposable = totalDisposable.add(net);
            if (income.compareTo(BigDecimal.ZERO) > 0 || expense.compareTo(BigDecimal.ZERO) > 0)
                monthsWithData++;
            // Stocké par mois calendaire : Avril 2025 → clé 4
            netByCalendarMonth.put(monthStart.getMonthValue(), net.doubleValue());
        }

        BigDecimal avgDisposable = totalDisposable.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        // Convertir les nets absolus en facteurs relatifs à la moyenne
        java.util.Map<Integer, Double> factorByCalendarMonth = new java.util.HashMap<>();
        double avgDouble = avgDisposable.compareTo(BigDecimal.ZERO) > 0 ? avgDisposable.doubleValue() : 1.0;
        netByCalendarMonth.forEach((month, net) ->
                factorByCalendarMonth.put(month, net / avgDouble));

        int planMonths = remaining.compareTo(BigDecimal.ZERO) > 0 && avgDisposable.compareTo(BigDecimal.ZERO) > 0
                ? remaining.divide(avgDisposable.multiply(BigDecimal.valueOf(0.2)).max(BigDecimal.ONE),
                0, RoundingMode.CEILING).intValue()
                : 12;
        planMonths = Math.min(Math.max(planMonths, 1), 36);

        BigDecimal recommendedMonthly = remaining.compareTo(BigDecimal.ZERO) > 0
                ? remaining.divide(BigDecimal.valueOf(planMonths), 2, RoundingMode.CEILING)
                : BigDecimal.ZERO;

        List<SmartSavingsPlanResponse.MonthlyPlan> monthlyPlan = new ArrayList<>();
        BigDecimal projectedBalance = currentBalance;

        for (int i = 1; i <= planMonths; i++) {
            LocalDate planMonth = now.plusMonths(i);
            // Facteur basé sur le MÊME mois calendaire de l'année passée (Avril 2026 → données Avril 2025)
            double factor = factorByCalendarMonth.getOrDefault(planMonth.getMonthValue(), 1.0);
            factor = Math.max(0.5, Math.min(factor, 2.0)); // Clamp [0.5 … 2.0]

            BigDecimal adjusted = recommendedMonthly
                    .multiply(BigDecimal.valueOf(factor))
                    .setScale(2, RoundingMode.HALF_UP);
            projectedBalance = projectedBalance.add(adjusted);

            double progress = goalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? projectedBalance.min(goalAmount)
                    .divide(goalAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0;

            String monthName = planMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
            String note;
            if (factor > 1.3) {
                note = String.format(
                        "Mois favorable : en %s l'an dernier vos revenus nets étaient %.0f%% au-dessus de la moyenne. " +
                                "Profitez-en pour épargner %.0f TND ce mois-ci (vs %.0f TND en rythme normal).",
                        monthName, (factor - 1.0) * 100, adjusted.doubleValue(), recommendedMonthly.doubleValue());
            } else if (factor > 0.9) {
                note = String.format(
                        "Mois standard : activité proche de la moyenne annuelle. " +
                                "Maintenez votre dépôt habituel de %.0f TND sans effort particulier.",
                        adjusted.doubleValue());
            } else {
                note = String.format(
                        "Mois difficile : en %s l'an dernier vos dépenses nettes étaient %.0f%% au-dessus de la moyenne. " +
                                "Montant réduit à %.0f TND (vs %.0f TND normalement) pour ne pas forcer votre budget.",
                        monthName, (1.0 - factor) * 100, adjusted.doubleValue(), recommendedMonthly.doubleValue());
            }

            monthlyPlan.add(SmartSavingsPlanResponse.MonthlyPlan.builder()
                    .month(monthName + " " + planMonth.getYear())
                    .recommendedSavings(adjusted)
                    .projectedBalance(projectedBalance.min(goalAmount))
                    .progressPercentage(Math.min(100, Math.round(progress * 100.0) / 100.0))
                    .seasonalNote(note)
                    .build());

            if (projectedBalance.compareTo(goalAmount) >= 0) break;
        }

        String dataSource = monthsWithData == 0
                ? "Aucune écriture comptable trouvée sur 12 mois — facteur saisonnier neutre (1.0) appliqué."
                : monthsWithData + " mois actifs sur 12 analysés (écritures réelles de " + historicalFrom
                + " à " + now + "). Facteurs saisonniers = revenu net mensuel réel / moyenne annuelle.";

        return SmartSavingsPlanResponse.builder()
                .historicalPeriodFrom(historicalFrom)
                .historicalPeriodTo(now)
                .monthsWithAccountingData(monthsWithData)
                .averageMonthlyDisposableIncome(avgDisposable)
                .dataSource(dataSource)
                .goalAmount(goalAmount)
                .currentBalance(currentBalance)
                .remaining(remaining)
                .recommendedMonthlySavings(recommendedMonthly)
                .planDurationMonths(monthlyPlan.size())
                .monthlyPlan(monthlyPlan)
                .build();
    }

    // ======================================================================
    // Feature 9 : Score de risque d'un retrait
    // ======================================================================

    @Override
    public WithdrawalRiskScoreResponse assessWithdrawalRisk(String userId, BigDecimal amount) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        BigDecimal currentBalance = account.getCurrentBalance();
        BigDecimal balanceAfter = currentBalance.subtract(amount);
        LocalDate now = LocalDate.now();

        // Facteur 1 : Impact sur les objectifs (poids 35%)
        int goalImpactScore = 1;
        if (account.getGoalAmount() != null && account.getGoalAmount().compareTo(BigDecimal.ZERO) > 0) {
            double progressBefore = currentBalance.doubleValue() / account.getGoalAmount().doubleValue() * 100;
            double progressAfter = Math.max(0, balanceAfter.doubleValue()) / account.getGoalAmount().doubleValue() * 100;
            double drop = progressBefore - progressAfter;
            if (drop > 50) goalImpactScore = 10;
            else if (drop > 30) goalImpactScore = 8;
            else if (drop > 15) goalImpactScore = 6;
            else if (drop > 5) goalImpactScore = 4;
            else goalImpactScore = 2;
        }

        // Facteur 2 : Couverture des dépenses réelles (poids 25%)
        BigDecimal expenses3m = accountingEntryRepository.sumAmountByUserIdAndTypeAndDateRange(
                userId, EntryType.EXPENSE, now.minusMonths(3), now);
        BigDecimal monthlyExpenses = expenses3m.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        int expenseCoverageScore = 1;
        if (monthlyExpenses.compareTo(BigDecimal.ZERO) > 0) {
            double coverageMonths = balanceAfter.doubleValue() / monthlyExpenses.doubleValue();
            if (coverageMonths < 0) expenseCoverageScore = 10;
            else if (coverageMonths < 1) expenseCoverageScore = 8;
            else if (coverageMonths < 2) expenseCoverageScore = 6;
            else if (coverageMonths < 3) expenseCoverageScore = 4;
            else expenseCoverageScore = 2;
        }

        // Facteur 3 : Part du solde retirée (poids 20%)
        int bufferScore = 1;
        if (currentBalance.compareTo(BigDecimal.ZERO) > 0) {
            double pct = amount.doubleValue() / currentBalance.doubleValue() * 100;
            if (pct > 80) bufferScore = 10;
            else if (pct > 60) bufferScore = 8;
            else if (pct > 40) bufferScore = 6;
            else if (pct > 20) bufferScore = 4;
            else bufferScore = 2;
        }

        // Facteur 4 : Historique des transactions (poids 20%)
        int historyScore = 5;
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        Long recentDeposits = transactionRepository.countByAccountIdAndTypeAndDateRange(
                account.getId(), SavingsTransactionType.DEPOSIT, threeMonthsAgo, LocalDateTime.now());
        Long recentWithdrawals = transactionRepository.countByAccountIdAndTypeAndDateRange(
                account.getId(), SavingsTransactionType.WITHDRAWAL, threeMonthsAgo, LocalDateTime.now());
        if (recentDeposits > recentWithdrawals * 2) historyScore = 2;
        else if (recentDeposits > recentWithdrawals) historyScore = 4;
        else if (recentWithdrawals > recentDeposits * 2) historyScore = 9;
        else historyScore = 6;

        double weightedScore = goalImpactScore * 0.35 + expenseCoverageScore * 0.25
                + bufferScore * 0.20 + historyScore * 0.20;
        int riskScore = (int) Math.round(Math.min(10, Math.max(1, weightedScore)));

        String riskLevel = riskScore >= 8 ? "CRITICAL" : riskScore >= 6 ? "HIGH" : riskScore >= 4 ? "MEDIUM" : "LOW";

        List<String> warnings = new ArrayList<>();
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0)
            warnings.add(String.format("Solde insuffisant : vous avez %.0f TND mais souhaitez retirer %.0f TND — opération impossible.",
                    currentBalance.doubleValue(), amount.doubleValue()));
        if (goalImpactScore >= 7) {
            double progressBefore = account.getGoalAmount() != null && account.getGoalAmount().compareTo(BigDecimal.ZERO) > 0
                    ? currentBalance.doubleValue() / account.getGoalAmount().doubleValue() * 100 : 0;
            double progressAfter = account.getGoalAmount() != null && account.getGoalAmount().compareTo(BigDecimal.ZERO) > 0
                    ? Math.max(0, balanceAfter.doubleValue()) / account.getGoalAmount().doubleValue() * 100 : 0;
            warnings.add(String.format("Impact objectifs : progression réduite de %.0f%% à %.0f%% — recul de %.0f points sur votre cible de %.0f TND.",
                    progressBefore, progressAfter, progressBefore - progressAfter,
                    account.getGoalAmount() != null ? account.getGoalAmount().doubleValue() : 0));
        }
        if (expenseCoverageScore >= 7)
            warnings.add(String.format("Couverture insuffisante : après ce retrait, %.0f TND restants ne couvrent que %.1f mois de vos dépenses réelles (%.0f TND/mois). Le minimum recommandé est 3 mois.",
                    balanceAfter.max(BigDecimal.ZERO).doubleValue(),
                    monthlyExpenses.compareTo(BigDecimal.ZERO) > 0 ? balanceAfter.max(BigDecimal.ZERO).divide(monthlyExpenses, 1, RoundingMode.HALF_UP).doubleValue() : 0,
                    monthlyExpenses.doubleValue()));
        if (bufferScore >= 7) {
            double pct = currentBalance.compareTo(BigDecimal.ZERO) > 0
                    ? amount.doubleValue() / currentBalance.doubleValue() * 100 : 0;
            warnings.add(String.format("Retrait massif : %.0f%% de votre épargne totale (%.0f TND sur %.0f TND). Il ne resterait que %.0f TND.",
                    pct, amount.doubleValue(), currentBalance.doubleValue(), balanceAfter.max(BigDecimal.ZERO).doubleValue()));
        }
        if (historyScore >= 8)
            warnings.add(String.format("Tendance préoccupante : %d retraits vs %d dépôts sur les 3 derniers mois — votre épargne s'érode régulièrement. Réévaluez vos habitudes avant ce retrait supplémentaire.",
                    recentWithdrawals.intValue(), recentDeposits.intValue()));

        double coverageAfter = monthlyExpenses.compareTo(BigDecimal.ZERO) > 0 && balanceAfter.compareTo(BigDecimal.ZERO) > 0
                ? balanceAfter.divide(monthlyExpenses, 1, RoundingMode.HALF_UP).doubleValue() : 0;

        String recommendation;
        if (riskScore >= 8) {
            recommendation = String.format(
                    "Retrait fortement déconseillé : retirer %.0f TND ne laisserait que %.0f TND sur votre compte — " +
                            "soit %.1f mois de dépenses (%.0f TND/mois). Vos objectifs d'épargne seraient retardés de façon significative. " +
                            "Si le besoin est urgent, envisagez un montant maximum de %.0f TND (20%% du solde).",
                    amount.doubleValue(), balanceAfter.max(BigDecimal.ZERO).doubleValue(), coverageAfter,
                    monthlyExpenses.doubleValue(), currentBalance.multiply(BigDecimal.valueOf(0.20)).setScale(0, RoundingMode.HALF_UP).doubleValue());
        } else if (riskScore >= 6) {
            BigDecimal saferAmount = currentBalance.multiply(BigDecimal.valueOf(0.30)).setScale(0, RoundingMode.HALF_UP);
            recommendation = String.format(
                    "Retrait risqué : après %.0f TND retirés, il resterait %.0f TND (%.1f mois de dépenses réelles à %.0f TND/mois). " +
                            "Envisagez plutôt %.0f TND (30%% du solde) pour conserver une marge de sécurité suffisante.",
                    amount.doubleValue(), balanceAfter.max(BigDecimal.ZERO).doubleValue(), coverageAfter,
                    monthlyExpenses.doubleValue(), saferAmount.doubleValue());
        } else if (riskScore >= 4) {
            recommendation = String.format(
                    "Retrait acceptable : %.0f TND restants après l'opération couvriront %.1f mois de vos dépenses réelles (%.0f TND/mois). " +
                            "Planifiez un dépôt dans les 4-6 semaines suivantes pour reconstituer votre épargne.",
                    balanceAfter.doubleValue(), coverageAfter, monthlyExpenses.doubleValue());
        } else {
            recommendation = String.format(
                    "Retrait à faible risque : %.0f TND restants représentent %.1f mois de dépenses (%.0f TND/mois). " +
                            "Vos objectifs d'épargne restent bien engagés. Pensez à maintenir vos dépôts réguliers.",
                    balanceAfter.doubleValue(), coverageAfter, monthlyExpenses.doubleValue());
        }

        return WithdrawalRiskScoreResponse.builder()
                .withdrawalAmount(amount)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .recommendation(recommendation)
                .goalImpact(buildFactor("Impact sur les objectifs", goalImpactScore, 0.35,
                        "Chute de progression vers vos objectifs après le retrait"))
                .expenseCoverage(buildFactor("Couverture des dépenses réelles", expenseCoverageScore, 0.25,
                        "Basé sur vos " + expenses3m + " DT de dépenses réelles sur 3 mois (" + monthlyExpenses + " DT/mois)"))
                .bufferAnalysis(buildFactor("Tampon restant", bufferScore, 0.20,
                        "Vous retirez " + amount + " DT sur " + currentBalance + " DT de solde"))
                .savingsHistory(buildFactor("Historique (3 mois)", historyScore, 0.20,
                        recentDeposits + " dépôts vs " + recentWithdrawals + " retraits sur les 3 derniers mois"))
                .warnings(warnings)
                .build();
    }

    private WithdrawalRiskScoreResponse.RiskFactor buildFactor(String name, int score, double weight, String detail) {
        return WithdrawalRiskScoreResponse.RiskFactor.builder()
                .name(name).score(score).weight(weight).detail(detail).build();
    }

    // ======================================================================
    // Feature 10 : Calculateur de fonds d'urgence (données réelles détaillées)
    // ======================================================================

    @Override
    public EmergencyFundCalculatorResponse calculateEmergencyFund(String userId) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        LocalDate today = LocalDate.now();
        LocalDate periodFrom = today.minusMonths(6).withDayOfMonth(1);
        LocalDate periodTo = today;

        // Détail mois par mois des dépenses réelles
        List<EmergencyFundCalculatorResponse.MonthlyExpenseDetail> monthlyDetails = new ArrayList<>();
        BigDecimal totalExpenses6m = BigDecimal.ZERO;
        int monthsWithData = 0;

        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            BigDecimal monthExpense = accountingEntryRepository.sumAmountByUserIdAndTypeAndDateRange(
                    userId, EntryType.EXPENSE, monthStart, monthEnd);
            Long entryCount = accountingEntryRepository.countByUserIdAndTypeAndDateRange(
                    userId, EntryType.EXPENSE, monthStart, monthEnd);

            totalExpenses6m = totalExpenses6m.add(monthExpense);
            if (monthExpense.compareTo(BigDecimal.ZERO) > 0) monthsWithData++;

            monthlyDetails.add(EmergencyFundCalculatorResponse.MonthlyExpenseDetail.builder()
                    .month(monthStart.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)
                            + " " + monthStart.getYear())
                    .totalExpenses(monthExpense)
                    .numberOfEntries(entryCount != null ? entryCount.intValue() : 0)
                    .build());
        }

        // Moyenne sur les 6 mois (même si certains mois n'ont pas de données — c'est voulu)
        BigDecimal avgMonthlyExpenses = totalExpenses6m.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);

        BigDecimal minimum = avgMonthlyExpenses.multiply(BigDecimal.valueOf(3));
        BigDecimal optimal = avgMonthlyExpenses.multiply(BigDecimal.valueOf(6));
        BigDecimal currentSavings = account.getCurrentBalance();

        BigDecimal deficit = minimum.subtract(currentSavings).max(BigDecimal.ZERO);
        BigDecimal surplus = currentSavings.subtract(optimal).max(BigDecimal.ZERO);

        double coverageMonths = avgMonthlyExpenses.compareTo(BigDecimal.ZERO) > 0
                ? currentSavings.divide(avgMonthlyExpenses, 2, RoundingMode.HALF_UP).doubleValue() : 0;

        String protectionLevel = coverageMonths >= 6 ? "EXCELLENT"
                : coverageMonths >= 4 ? "GOOD"
                : coverageMonths >= 2 ? "MODERATE"
                : coverageMonths >= 1 ? "LOW" : "NONE";

        double protectionPct = optimal.compareTo(BigDecimal.ZERO) > 0
                ? Math.min(100, currentSavings.divide(optimal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue())
                : 0;

        // Contributions mensuelles nécessaires
        BigDecimal need3Months = minimum.subtract(currentSavings).max(BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(6), 2, RoundingMode.CEILING);
        BigDecimal need6Months = optimal.subtract(currentSavings).max(BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.CEILING);

        String recommendation;
        if ("EXCELLENT".equals(protectionLevel)) {
            recommendation = String.format(
                    "Excellent ! Votre épargne (%.0f TND) couvre %.1f mois de dépenses réelles (%.0f TND/mois) — " +
                            "bien au-delà du seuil optimal de 6 mois (%.0f TND). " +
                            "L'excédent de %.0f TND au-dessus de l'optimal peut être investi dans la croissance de votre exploitation.",
                    currentSavings.doubleValue(), coverageMonths, avgMonthlyExpenses.doubleValue(),
                    optimal.doubleValue(), surplus.doubleValue());
        } else if ("GOOD".equals(protectionLevel)) {
            recommendation = String.format(
                    "Bon niveau : %.0f TND d'épargne couvre %.1f mois de vos dépenses réelles (%.0f TND/mois). " +
                            "Il vous manque %.0f TND pour atteindre l'objectif optimal de 6 mois (%.0f TND). " +
                            "En épargnant %.0f TND/mois, vous l'atteignez en 12 mois.",
                    currentSavings.doubleValue(), coverageMonths, avgMonthlyExpenses.doubleValue(),
                    optimal.subtract(currentSavings).max(BigDecimal.ZERO).doubleValue(), optimal.doubleValue(), need6Months.doubleValue());
        } else if ("MODERATE".equals(protectionLevel)) {
            recommendation = String.format(
                    "Protection modérée : %.0f TND d'épargne couvre %.1f mois (minimum recommandé : 3 mois = %.0f TND). " +
                            "Il vous manque %.0f TND pour atteindre ce minimum. " +
                            "En épargnant %.0f TND/mois, vous comblerez ce déficit en 6 mois.",
                    currentSavings.doubleValue(), coverageMonths, minimum.doubleValue(),
                    deficit.doubleValue(), need3Months.doubleValue());
        } else {
            recommendation = String.format(
                    "Protection insuffisante : %.0f TND d'épargne couvre seulement %.1f mois (minimum vital : %.0f TND = 3 × %.0f TND/mois). " +
                            "Priorité absolue — il vous manque %.0f TND. Épargnez %.0f TND/mois pour atteindre ce minimum en 6 mois. " +
                            "En cas de panne matériel ou mauvaise récolte, ce fonds sera votre seule protection.",
                    currentSavings.doubleValue(), coverageMonths, minimum.doubleValue(), avgMonthlyExpenses.doubleValue(),
                    deficit.doubleValue(), need3Months.doubleValue());
        }

        return EmergencyFundCalculatorResponse.builder()
                .totalExpensesLast6Months(totalExpenses6m)
                .monthsWithExpenseData(monthsWithData)
                .periodFrom(periodFrom)
                .periodTo(periodTo)
                .monthlyExpenseDetails(monthlyDetails)
                .averageMonthlyExpenses(avgMonthlyExpenses)
                .recommendedMinimum(minimum)
                .recommendedOptimal(optimal)
                .currentSavings(currentSavings)
                .deficit(deficit)
                .surplusAboveOptimal(surplus)
                .coverageMonths(Math.round(coverageMonths * 100.0) / 100.0)
                .protectionLevel(protectionLevel)
                .protectionPercentage(Math.round(protectionPct * 100.0) / 100.0)
                .monthlyContributionNeeded3Months(need3Months)
                .monthlyContributionNeeded6Months(need6Months)
                .recommendation(recommendation)
                .build();
    }
}
