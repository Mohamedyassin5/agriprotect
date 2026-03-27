package tn.esprit.agri.services;

import tn.esprit.agri.dto_savings_accountability.*;

public interface IAccountingAIService {

    FinancialHealthScoreResponse getFinancialHealthScore(String userId);

    ExpenseForecastResponse getExpenseForecast(String userId, Integer months);

    WhatIfSimulationResponse simulateWhatIf(String userId, WhatIfSimulationRequest request);

    ProfitabilityTrendResponse getProfitabilityTrend(String userId, Integer months);

    SmartCategorizationResponse categorize(String description);

    PredictiveBudgetAlertResponse getPredictiveBudgetAlerts(String userId);
}
