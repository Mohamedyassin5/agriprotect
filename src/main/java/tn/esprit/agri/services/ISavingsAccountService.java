package tn.esprit.agri.services;

import tn.esprit.agri.dto_savings_accountability.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ISavingsAccountService {
    // CRUD operations
    SavingsAccountResponse create(String userId, SavingsAccountRequest request);
    SavingsAccountResponse getMyAccount(String userId);
    SavingsAccountResponse update(String userId, SavingsAccountRequest request);
    void delete(String userId);

    // Business operations
    GoalProgressResponse getGoalProgress(String userId);
    MonthlySummaryResponse getMonthlySummary(String userId, Integer months);
    AlertsResponse getAlerts(String userId);
    SimulateWithdrawResponse simulateWithdraw(String userId, BigDecimal amount);
    RecommendationResponse getRecommendation(String userId);
    StatementResponse getStatement(String userId, LocalDate from, LocalDate to);
}
