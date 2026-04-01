package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.dto_savings_accountability.*;
import tn.esprit.agri.entities.AccountingEntry;
import tn.esprit.agri.entities.Budget;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.EntryCategory;
import tn.esprit.agri.entities.enums.EntryType;
import tn.esprit.agri.repositories.AccountingEntryRepository;
import tn.esprit.agri.repositories.BudgetRepository;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.IAccountingEntryService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountingEntryServiceImpl implements IAccountingEntryService {

    private final AccountingEntryRepository entryRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    @Override
    public EntryResponse create(String userId, EntryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AccountingEntry entry = new AccountingEntry();
        entry.setUser(user);
        entry.setEntryType(request.getEntryType());
        entry.setCategory(request.getCategory());
        entry.setAmount(request.getAmount());
        entry.setDescription(request.getDescription());
        entry.setEntryDate(request.getEntryDate());

        AccountingEntry saved = entryRepository.save(entry);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntryResponse> getEntries(String userId, EntryType type, EntryCategory category,
                                          LocalDate from, LocalDate to, Pageable pageable) {
        Page<AccountingEntry> entries = entryRepository.findByFilters(userId, type, category, from, to, pageable);
        return entries.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EntryResponse getById(Long id, String userId) {
        AccountingEntry entry = entryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Entry not found or access denied"));
        return toResponse(entry);
    }

    @Override
    public EntryResponse update(Long id, String userId, EntryRequest request) {
        AccountingEntry entry = entryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Entry not found or access denied"));

        if (request.getEntryType() != null) entry.setEntryType(request.getEntryType());
        if (request.getCategory() != null) entry.setCategory(request.getCategory());
        if (request.getAmount() != null) entry.setAmount(request.getAmount());
        if (request.getDescription() != null) entry.setDescription(request.getDescription());
        if (request.getEntryDate() != null) entry.setEntryDate(request.getEntryDate());

        AccountingEntry updated = entryRepository.save(entry);
        return toResponse(updated);
    }

    @Override
    public void delete(Long id, String userId) {
        AccountingEntry entry = entryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Entry not found or access denied"));
        entryRepository.delete(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public SummaryResponse getSummary(String userId, LocalDate from, LocalDate to) {
        BigDecimal totalIncome;
        BigDecimal totalExpense;
        Long incomeCount;
        Long expenseCount;

        if (from != null && to != null) {
            totalIncome = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.INCOME, from, to);
            totalExpense = entryRepository.sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, from, to);
            incomeCount = entryRepository.countByUserIdAndTypeAndDateRange(userId, EntryType.INCOME, from, to);
            expenseCount = entryRepository.countByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, from, to);
        } else {
            totalIncome = entryRepository.sumAmountByUserIdAndType(userId, EntryType.INCOME);
            totalExpense = entryRepository.sumAmountByUserIdAndType(userId, EntryType.EXPENSE);
            incomeCount = 0L;
            expenseCount = 0L;
        }

        SummaryResponse response = new SummaryResponse();
        response.setPeriodStart(from);
        response.setPeriodEnd(to);
        response.setTotalIncome(totalIncome);
        response.setTotalExpenses(totalExpense);
        response.setNetIncome(totalIncome.subtract(totalExpense));
        response.setIncomeTransactionCount(incomeCount.intValue());
        response.setExpenseTransactionCount(expenseCount.intValue());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetVsActualResponse getBudgetVsActual(String userId, LocalDate periodStart, LocalDate periodEnd) {
        List<Budget> budgets = budgetRepository.findByUserIdAndPeriodStartBetween(userId, periodStart, periodEnd);

        List<BudgetVsActualResponse.CategoryComparison> comparisons = new ArrayList<>();
        BigDecimal totalBudgeted = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;

        for (Budget budget : budgets) {
            BigDecimal actualSpent = entryRepository.sumAmountByCategoryAndDateRange(
                    userId, EntryType.EXPENSE, budget.getCategory(),
                    budget.getPeriodStart(), budget.getPeriodEnd());

            BigDecimal variance = budget.getPlannedAmount().subtract(actualSpent);
            double percentageUsed = budget.getPlannedAmount().compareTo(BigDecimal.ZERO) > 0
                    ? actualSpent.divide(budget.getPlannedAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;
            String status = variance.compareTo(BigDecimal.ZERO) >= 0 ? "OK" :
                    variance.abs().compareTo(budget.getPlannedAmount().multiply(new BigDecimal("0.1"))) <= 0 ? "WARN" : "ALERT";

            comparisons.add(BudgetVsActualResponse.CategoryComparison.builder()
                    .category(budget.getCategory())
                    .budgetedAmount(budget.getPlannedAmount())
                    .actualAmount(actualSpent)
                    .variance(variance)
                    .percentageUsed(percentageUsed)
                    .status(status)
                    .build());

            totalBudgeted = totalBudgeted.add(budget.getPlannedAmount());
            totalActual = totalActual.add(actualSpent);
        }

        return BudgetVsActualResponse.builder()
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .comparisons(comparisons)
                .totalBudgeted(totalBudgeted)
                .totalActual(totalActual)
                .totalVariance(totalBudgeted.subtract(totalActual))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SpendingBreakdownResponse getSpendingBreakdown(String userId, LocalDate from, LocalDate to) {
        List<SpendingBreakdownResponse.CategoryBreakdown> breakdownList = new ArrayList<>();
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (EntryCategory category : EntryCategory.values()) {
            BigDecimal total = entryRepository.sumAmountByCategoryAndDateRange(
                    userId, EntryType.EXPENSE, category, from, to);
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                Long count = entryRepository.countByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, from, to);
                breakdownList.add(SpendingBreakdownResponse.CategoryBreakdown.builder()
                        .category(category)
                        .amount(total)
                        .transactionCount(count.intValue())
                        .build());
                totalExpenses = totalExpenses.add(total);
            }
        }

        for (SpendingBreakdownResponse.CategoryBreakdown cb : breakdownList) {
            if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                cb.setPercentage(cb.getAmount()
                        .divide(totalExpenses, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue());
            } else {
                cb.setPercentage(0.0);
            }
        }

        return SpendingBreakdownResponse.builder()
                .periodStart(from)
                .periodEnd(to)
                .totalExpenses(totalExpenses)
                .breakdown(breakdownList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CashflowForecastResponse getCashflowForecast(String userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(3);

        BigDecimal totalIncome = entryRepository.sumAmountByUserIdAndTypeAndDateRange(
                userId, EntryType.INCOME, startDate, endDate);
        BigDecimal totalExpense = entryRepository.sumAmountByUserIdAndTypeAndDateRange(
                userId, EntryType.EXPENSE, startDate, endDate);

        BigDecimal avgMonthlyIncome = totalIncome.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal avgMonthlyExpenses = totalExpense.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

        List<CashflowForecastResponse.MonthlyForecast> forecasts = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;

        for (int i = 1; i <= 3; i++) {
            LocalDate forecastStart = endDate.plusMonths(i).withDayOfMonth(1);
            BigDecimal netCashflow = avgMonthlyIncome.subtract(avgMonthlyExpenses);
            cumulative = cumulative.add(netCashflow);

            forecasts.add(CashflowForecastResponse.MonthlyForecast.builder()
                    .month(forecastStart.getMonth().name() + " " + forecastStart.getYear())
                    .periodStart(forecastStart)
                    .projectedIncome(avgMonthlyIncome)
                    .projectedExpenses(avgMonthlyExpenses)
                    .projectedNetCashflow(netCashflow)
                    .cumulativeCashflow(cumulative)
                    .build());
        }

        return CashflowForecastResponse.builder()
                .currentBalance(totalIncome.subtract(totalExpense))
                .averageMonthlyIncome(avgMonthlyIncome)
                .averageMonthlyExpenses(avgMonthlyExpenses)
                .forecasts(forecasts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OverspendingAlertResponse getOverspendingAlerts(String userId, LocalDate periodStart, LocalDate periodEnd) {
        List<Budget> budgets = budgetRepository.findByUserIdAndPeriodStartBetween(userId, periodStart, periodEnd);
        List<OverspendingAlertResponse.Alert> alerts = new ArrayList<>();

        for (Budget budget : budgets) {
            BigDecimal actualSpent = entryRepository.sumAmountByCategoryAndDateRange(
                    userId, EntryType.EXPENSE, budget.getCategory(),
                    budget.getPeriodStart(), budget.getPeriodEnd());

            if (actualSpent.compareTo(budget.getPlannedAmount()) > 0) {
                BigDecimal overspending = actualSpent.subtract(budget.getPlannedAmount());
                double percentageOver = budget.getPlannedAmount().compareTo(BigDecimal.ZERO) > 0
                        ? overspending.divide(budget.getPlannedAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue()
                        : 0.0;
                String severity = percentageOver > 50 ? "CRITICAL" : percentageOver > 20 ? "HIGH" : "MEDIUM";

                alerts.add(OverspendingAlertResponse.Alert.builder()
                        .category(budget.getCategory())
                        .budgetedAmount(budget.getPlannedAmount())
                        .actualAmount(actualSpent)
                        .overspending(overspending)
                        .percentageOver(percentageOver)
                        .severity(severity)
                        .build());
            }
        }

        return OverspendingAlertResponse.builder()
                .alerts(alerts)
                .totalAlertsCount(alerts.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AnomalyResponse getAnomalies(String userId, LocalDate from, LocalDate to) {
        List<AccountingEntry> entries = entryRepository.findByUserIdAndEntryDateBetween(userId, from, to);
        List<AnomalyResponse.Anomaly> anomalies = new ArrayList<>();

        for (EntryCategory category : EntryCategory.values()) {
            BigDecimal avgAmount = entryRepository.averageAmountByCategoryAndDateRange(
                    userId, EntryType.EXPENSE, category, from, to);

            if (avgAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal threshold = avgAmount.multiply(new BigDecimal("2"));

                entries.stream()
                        .filter(e -> e.getCategory() == category && e.getEntryType() == EntryType.EXPENSE)
                        .filter(e -> e.getAmount().compareTo(threshold) > 0)
                        .forEach(e -> {
                            double deviationPct = e.getAmount().subtract(avgAmount)
                                    .divide(avgAmount, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)).doubleValue();

                            anomalies.add(AnomalyResponse.Anomaly.builder()
                                    .entryId(e.getId())
                                    .entryDate(e.getEntryDate())
                                    .category(e.getCategory())
                                    .amount(e.getAmount())
                                    .averageAmount(avgAmount)
                                    .deviationPercentage(deviationPct)
                                    .reason("Amount exceeds 2x category average")
                                    .build());
                        });
            }
        }

        return AnomalyResponse.builder()
                .anomalies(anomalies)
                .totalAnomaliesCount(anomalies.size())
                .build();
    }

    private EntryResponse toResponse(AccountingEntry entry) {
        EntryResponse response = new EntryResponse();
        response.setId(entry.getId());
        response.setEntryType(entry.getEntryType());
        response.setCategory(entry.getCategory());
        response.setAmount(entry.getAmount());
        response.setDescription(entry.getDescription());
        response.setEntryDate(entry.getEntryDate());
        response.setSource(entry.getSource());
        response.setCreatedAt(entry.getCreatedAt());
        return response;
    }
}
