package tn.esprit.agri.services;

import tn.esprit.agri.dto_savings_accountability.*;

import java.math.BigDecimal;

public interface ISavingsAIService {

    GoalAchievementPredictionResponse predictGoalAchievement(String userId);

    SmartSavingsPlanResponse getSmartSavingsPlan(String userId);

    WithdrawalRiskScoreResponse assessWithdrawalRisk(String userId, BigDecimal amount);

    EmergencyFundCalculatorResponse calculateEmergencyFund(String userId);
}
