package tn.esprit.agri.services;

import tn.esprit.agri.dto_savings_accountability.CashFlowOptimizerResponse;
import tn.esprit.agri.dto_savings_accountability.DecisionDashboardResponse;

public interface IDecisionDashboardService {

    DecisionDashboardResponse getDashboard(String userId);

    CashFlowOptimizerResponse optimizeCashFlow(String userId);
}
