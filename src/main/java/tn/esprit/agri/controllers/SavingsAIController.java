package tn.esprit.agri.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.dto_savings_accountability.*;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.services.ISavingsAIService;

@RestController
@RequestMapping("/api/ai/savings")
@RequiredArgsConstructor
public class SavingsAIController {

    private final ISavingsAIService savingsAIService;

    // Feature 7: Goal Achievement Predictor
    @GetMapping("/goal/predict-achievement")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<GoalAchievementPredictionResponse> predictGoalAchievement(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(savingsAIService.predictGoalAchievement(user.getId()));
    }

    // Feature 8: Smart Savings Plan
    @GetMapping("/plan/smart")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<SmartSavingsPlanResponse> getSmartSavingsPlan(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(savingsAIService.getSmartSavingsPlan(user.getId()));
    }

    // Feature 9: Withdrawal Risk Assessment
    @PostMapping("/withdrawal/risk-assessment")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<WithdrawalRiskScoreResponse> assessWithdrawalRisk(
            @AuthenticationPrincipal User user,
            @RequestBody WithdrawalRiskRequest request) {
        return ResponseEntity.ok(savingsAIService.assessWithdrawalRisk(user.getId(), request.getAmount()));
    }

    // Feature 10: Emergency Fund Calculator
    @GetMapping("/emergency-fund")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<EmergencyFundCalculatorResponse> calculateEmergencyFund(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(savingsAIService.calculateEmergencyFund(user.getId()));
    }
}
