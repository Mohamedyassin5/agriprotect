package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.dto_savings_accountability.*;
import tn.esprit.agri.entities.SavingsAccount;
import tn.esprit.agri.entities.SavingsTransaction;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.EntryType;
import tn.esprit.agri.entities.enums.SavingsAccountStatus;
import tn.esprit.agri.exception.ResourceNotFoundException;
import tn.esprit.agri.repositories.AccountingEntryRepository;
import tn.esprit.agri.repositories.SavingsAccountRepository;
import tn.esprit.agri.repositories.SavingsTransactionRepository;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.ISavingsAccountService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SavingsAccountServiceImpl implements ISavingsAccountService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsTransactionRepository transactionRepository;
    private final AccountingEntryRepository accountingEntryRepository;
    private final UserRepository userRepository;

    @Override
    public SavingsAccountResponse create(String userId, SavingsAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        savingsAccountRepository.findByUserId(userId)
                .ifPresent(account -> {
                    throw new IllegalArgumentException("User already has a savings account");
                });

        SavingsAccount account = SavingsAccount.builder()
                .user(user)
                .accountName(request.getAccountName() != null ? request.getAccountName() : "Mon compte epargne")
                .currentBalance(BigDecimal.ZERO)
                .monthlySavingsTarget(request.getMonthlySavingsTarget())
                .status(SavingsAccountStatus.ACTIVE)
                .build();

        account = savingsAccountRepository.save(account);
        return mapToResponse(account);
    }

    @Override
    public SavingsAccountResponse getMyAccount(String userId) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));
        return mapToResponse(account);
    }

    @Override
    public SavingsAccountResponse update(String userId, SavingsAccountRequest request) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        if (request.getAccountName() != null) {
            account.setAccountName(request.getAccountName());
        }
        if (request.getMonthlySavingsTarget() != null) {
            account.setMonthlySavingsTarget(request.getMonthlySavingsTarget());
        }
        if (request.getStatus() != null) {
            if (request.getStatus() == SavingsAccountStatus.CLOSED) {
                throw new IllegalArgumentException("Cannot close account via update. Use DELETE endpoint.");
            }
            account.setStatus(request.getStatus());
        }

        account = savingsAccountRepository.save(account);
        return mapToResponse(account);
    }

    @Override
    public void delete(String userId) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        if (account.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Cannot delete account with positive balance");
        }

        savingsAccountRepository.delete(account);
    }

    @Override
    public GoalProgressResponse getGoalProgress(String userId) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        BigDecimal goalAmount = account.getGoalAmount() != null ? account.getGoalAmount() : BigDecimal.ZERO;
        BigDecimal currentBalance = account.getCurrentBalance();
        BigDecimal remaining = goalAmount.subtract(currentBalance);

        double progressPercentage = goalAmount.compareTo(BigDecimal.ZERO) > 0
                ? currentBalance.divide(goalAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        String status = "In Progress";
        if (progressPercentage >= 100) {
            status = "Completed";
        } else if (progressPercentage >= 75) {
            status = "Near Goal";
        } else if (progressPercentage < 25) {
            status = "Just Started";
        }

        return GoalProgressResponse.builder()
                .accountId(account.getId())
                .goalTitle(account.getGoalTitle())
                .goalAmount(goalAmount)
                .currentBalance(currentBalance)
                .remaining(remaining.max(BigDecimal.ZERO))
                .progressPercentage(progressPercentage)
                .status(status)
                .build();
    }

    @Override
    public MonthlySummaryResponse getMonthlySummary(String userId, Integer months) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        int numMonths = months != null ? months : 6;
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(numMonths);

        List<SavingsTransaction> transactions = transactionRepository
                .findByAccountIdAndOccurredAtBetween(account.getId(), startDate, endDate);

        BigDecimal totalDeposits = transactions.stream()
                .filter(t -> t.getType().name().equals("DEPOSIT"))
                .map(SavingsTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdrawals = transactions.stream()
                .filter(t -> t.getType().name().equals("WITHDRAWAL"))
                .map(SavingsTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal startBalance = account.getCurrentBalance().subtract(totalDeposits).add(totalWithdrawals);

        // Build monthly breakdown - from startDate's month up to and including current month
        List<MonthlySummaryResponse.MonthlyData> monthlyData = new ArrayList<>();
        BigDecimal runningBalance = startBalance;

        LocalDateTime firstMonth = startDate.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime currentMonth = endDate.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        for (LocalDateTime ms = firstMonth; !ms.isAfter(currentMonth); ms = ms.plusMonths(1)) {
            final LocalDateTime monthStart = ms;
            final LocalDateTime monthEnd = ms.plusMonths(1).minusSeconds(1);

            BigDecimal monthDeposits = transactions.stream()
                    .filter(t -> t.getType().name().equals("DEPOSIT"))
                    .filter(t -> !t.getOccurredAt().isBefore(monthStart) && !t.getOccurredAt().isAfter(monthEnd))
                    .map(SavingsTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal monthWithdrawals = transactions.stream()
                    .filter(t -> t.getType().name().equals("WITHDRAWAL"))
                    .filter(t -> !t.getOccurredAt().isBefore(monthStart) && !t.getOccurredAt().isAfter(monthEnd))
                    .map(SavingsTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netChange = monthDeposits.subtract(monthWithdrawals);
            runningBalance = runningBalance.add(netChange);

            monthlyData.add(MonthlySummaryResponse.MonthlyData.builder()
                    .month(monthStart.getMonth().name() + " " + monthStart.getYear())
                    .deposits(monthDeposits)
                    .withdrawals(monthWithdrawals)
                    .netChange(netChange)
                    .balance(runningBalance)
                    .build());
        }

        return MonthlySummaryResponse.builder()
                .period(startDate.toLocalDate() + " to " + endDate.toLocalDate())
                .totalDeposits(totalDeposits)
                .totalWithdrawals(totalWithdrawals)
                .netChange(totalDeposits.subtract(totalWithdrawals))
                .startBalance(startBalance)
                .endBalance(account.getCurrentBalance())
                .transactionCount(transactions.size())
                .monthlyData(monthlyData)
                .build();
    }

    @Override
    public AlertsResponse getAlerts(String userId) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        List<AlertsResponse.Alert> alerts = new ArrayList<>();

        if (account.getCurrentBalance().compareTo(BigDecimal.valueOf(100)) < 0) {
            alerts.add(AlertsResponse.Alert.builder()
                    .type("LOW_BALANCE")
                    .severity("WARNING")
                    .message("Your savings balance is low")
                    .value(account.getCurrentBalance())
                    .build());
        }

        if (account.getGoalAmount() != null) {
            BigDecimal progress = account.getCurrentBalance()
                    .divide(account.getGoalAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (progress.compareTo(BigDecimal.valueOf(90)) >= 0) {
                alerts.add(AlertsResponse.Alert.builder()
                        .type("GOAL_NEAR")
                        .severity("INFO")
                        .message("You're close to reaching your savings goal!")
                        .value(progress)
                        .build());
            }
        }

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<SavingsTransaction> recentTransactions = transactionRepository
                .findByAccountIdAndOccurredAtBetween(account.getId(), oneMonthAgo, LocalDateTime.now());

        if (recentTransactions.isEmpty()) {
            alerts.add(AlertsResponse.Alert.builder()
                    .type("INACTIVITY")
                    .severity("INFO")
                    .message("No transactions in the last month")
                    .value(BigDecimal.ZERO)
                    .build());
        }

        return AlertsResponse.builder()
                .alerts(alerts)
                .totalAlerts(alerts.size())
                .build();
    }

    @Override
    public SimulateWithdrawResponse simulateWithdraw(String userId, BigDecimal amount) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        BigDecimal currentBalance = account.getCurrentBalance();
        BigDecimal balanceAfter = currentBalance.subtract(amount);
        boolean isAllowed = balanceAfter.compareTo(BigDecimal.ZERO) >= 0;

        String message = isAllowed
                ? "Withdrawal is allowed"
                : "Insufficient funds for this withdrawal";

        BigDecimal goalImpact = BigDecimal.ZERO;
        if (account.getGoalAmount() != null && isAllowed) {
            goalImpact = account.getGoalAmount().subtract(balanceAfter);
        }

        return SimulateWithdrawResponse.builder()
                .currentBalance(currentBalance)
                .withdrawalAmount(amount)
                .balanceAfterWithdrawal(balanceAfter)
                .isAllowed(isAllowed)
                .message(message)
                .goalAmount(account.getGoalAmount())
                .goalImpact(goalImpact)
                .build();
    }

    @Override
    public RecommendationResponse getRecommendation(String userId) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(3);

        BigDecimal totalIncome = accountingEntryRepository
                .sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.INCOME, startDate, endDate);

        BigDecimal totalExpenses = accountingEntryRepository
                .sumAmountByUserIdAndTypeAndDateRange(userId, EntryType.EXPENSE, startDate, endDate);

        BigDecimal monthlyIncome = totalIncome.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyExpenses = totalExpenses.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal disposableIncome = monthlyIncome.subtract(monthlyExpenses);

        BigDecimal recommendedSavings = disposableIncome.multiply(BigDecimal.valueOf(0.2));

        List<RecommendationResponse.Recommendation> recommendations = new ArrayList<>();

        if (disposableIncome.compareTo(BigDecimal.ZERO) > 0) {
            double currentRate = monthlyIncome.compareTo(BigDecimal.ZERO) > 0
                    ? disposableIncome.divide(monthlyIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;
            recommendations.add(RecommendationResponse.Recommendation.builder()
                    .category("SAVINGS_RATE")
                    .recommendation(String.format(
                            "Votre revenu disponible mensuel est de %.0f TND (revenus : %.0f TND − dépenses : %.0f TND). " +
                                    "En épargnant 20%% de ce disponible, vous mettez %.0f TND de côté chaque mois. " +
                                    "Actuellement votre taux d'épargne est de %.0f%% — %s",
                            disposableIncome.doubleValue(), monthlyIncome.doubleValue(), monthlyExpenses.doubleValue(),
                            recommendedSavings.doubleValue(), currentRate,
                            currentRate >= 20
                                    ? "objectif atteint ! Continuez à ce rythme."
                                    : String.format("il vous manque %.0f TND/mois pour atteindre cet objectif.", recommendedSavings.subtract(disposableIncome.multiply(BigDecimal.valueOf(currentRate / 100))).abs().doubleValue())))
                    .priority("HIGH")
                    .potentialSavings(recommendedSavings)
                    .build());
        }

        if (monthlyExpenses.compareTo(monthlyIncome.multiply(BigDecimal.valueOf(0.8))) > 0) {
            double ratio = monthlyIncome.compareTo(BigDecimal.ZERO) > 0
                    ? monthlyExpenses.divide(monthlyIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;
            BigDecimal targetExpenses = monthlyIncome.multiply(BigDecimal.valueOf(0.8)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal reduction = monthlyExpenses.subtract(targetExpenses).setScale(0, RoundingMode.HALF_UP);
            recommendations.add(RecommendationResponse.Recommendation.builder()
                    .category("EXPENSE_REDUCTION")
                    .recommendation(String.format(
                            "Vos dépenses (%.0f TND/mois) représentent %.0f%% de vos revenus (%.0f TND/mois) — au-dessus du seuil critique de 80%%. " +
                                    "Pour atteindre un ratio sain, réduisez de %.0f TND/mois (objectif : %.0f TND/mois). " +
                                    "Commencez par les catégories TRANSPORT et OTHER, généralement les plus flexibles.",
                            monthlyExpenses.doubleValue(), ratio, monthlyIncome.doubleValue(),
                            reduction.doubleValue(), targetExpenses.doubleValue()))
                    .priority("MEDIUM")
                    .potentialSavings(monthlyExpenses.multiply(BigDecimal.valueOf(0.1)))
                    .build());
        }

        return RecommendationResponse.builder()
                .currentSavingsBalance(account.getCurrentBalance())
                .monthlyIncome(monthlyIncome)
                .monthlyExpenses(monthlyExpenses)
                .recommendedMonthlySavings(recommendedSavings)
                .recommendations(recommendations)
                .build();
    }

    @Override
    public StatementResponse getStatement(String userId, LocalDate from, LocalDate to) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        LocalDateTime startDateTime = from.atStartOfDay();
        LocalDateTime endDateTime = to.atTime(23, 59, 59);

        List<SavingsTransaction> transactions = transactionRepository
                .findByAccountIdAndOccurredAtBetween(account.getId(), startDateTime, endDateTime);

        BigDecimal totalDeposits = transactions.stream()
                .filter(t -> t.getType().name().equals("DEPOSIT"))
                .map(SavingsTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdrawals = transactions.stream()
                .filter(t -> t.getType().name().equals("WITHDRAWAL"))
                .map(SavingsTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(this::mapTransactionToResponse)
                .collect(Collectors.toList());

        return StatementResponse.builder()
                .accountId(account.getId())
                .periodStart(from)
                .periodEnd(to)
                .openingBalance(account.getCurrentBalance().subtract(totalDeposits).add(totalWithdrawals))
                .closingBalance(account.getCurrentBalance())
                .totalDeposits(totalDeposits)
                .totalWithdrawals(totalWithdrawals)
                .transactions(transactionResponses)
                .build();
    }

    private SavingsAccountResponse mapToResponse(SavingsAccount account) {
        return SavingsAccountResponse.builder()
                .id(account.getId())
                .userId(account.getUser().getId())
                .accountName(account.getAccountName())
                .currentBalance(account.getCurrentBalance())
                .monthlySavingsTarget(account.getMonthlySavingsTarget())
                .goalAmount(account.getGoalAmount())
                .goalTitle(account.getGoalTitle())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private TransactionResponse mapTransactionToResponse(SavingsTransaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .accountId(transaction.getAccount().getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .occurredAt(transaction.getOccurredAt())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
