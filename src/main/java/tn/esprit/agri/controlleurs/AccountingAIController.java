package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.ai.client.FinanceAIClient;
import tn.esprit.agri.ai.dto.FinancialRiskRequest;
import tn.esprit.agri.ai.dto.FinancialRiskResponse;
import tn.esprit.agri.dto_savings_accountability.*;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.services.IAccountingAIService;

@RestController
@RequestMapping("/api/ai/accounting")
@RequiredArgsConstructor
public class AccountingAIController {

    private final IAccountingAIService accountingAIService;
    private final FinanceAIClient financeAIClient;

    // Feature 1: Financial Health Score
    @GetMapping("/health-score")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<FinancialHealthScoreResponse> getHealthScore(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(accountingAIService.getFinancialHealthScore(user.getId()));
    }

    // Feature 2: AI Expense Forecasting
    @GetMapping("/forecast/expenses")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ExpenseForecastResponse> getExpenseForecast(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Integer months) {
        return ResponseEntity.ok(accountingAIService.getExpenseForecast(user.getId(), months));
    }

    // Feature 3: What-If Budget Simulator
    @PostMapping("/simulate/what-if")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<WhatIfSimulationResponse> simulateWhatIf(
            @AuthenticationPrincipal User user,
            @RequestBody WhatIfSimulationRequest request) {
        return ResponseEntity.ok(accountingAIService.simulateWhatIf(user.getId(), request));
    }

    // Feature 4: Profitability Trend Analysis
    @GetMapping("/trends/profitability")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ProfitabilityTrendResponse> getProfitabilityTrend(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Integer months) {
        return ResponseEntity.ok(accountingAIService.getProfitabilityTrend(user.getId(), months));
    }

    // Feature 5: Smart Categorization
    @PostMapping("/categorize")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<SmartCategorizationResponse> categorize(@RequestBody SmartCategorizationRequest request) {
        return ResponseEntity.ok(accountingAIService.categorize(request.getDescription()));
    }

    // Feature 6: Predictive Budget Alerts
    @GetMapping("/budget/predictive-alerts")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<PredictiveBudgetAlertResponse> getPredictiveBudgetAlerts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(accountingAIService.getPredictiveBudgetAlerts(user.getId()));
    }

    // Feature ML: Financial Risk Prediction (Notebook Module 2)
    @PostMapping("/ml/risk-predict")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<FinancialRiskResponse> predictFinancialRisk(@RequestBody FinancialRiskRequest request) {
        FinancialRiskResponse response = financeAIClient.predictFinancialRisk(request);
        return ResponseEntity.ok(response);
    }
}
