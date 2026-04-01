package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.dto_savings_accountability.*;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.services.IDecisionDashboardService;

@RestController
@RequestMapping("/api/ai/dashboard")
@RequiredArgsConstructor
public class DecisionDashboardController {

    private final IDecisionDashboardService dashboardService;

    // Feature 11: Decision Dashboard
    @GetMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<DecisionDashboardResponse> getDashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getDashboard(user.getId()));
    }

    // Feature 12: Cash Flow Optimizer
    @GetMapping("/optimize-cashflow")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<CashFlowOptimizerResponse> optimizeCashFlow(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.optimizeCashFlow(user.getId()));
    }
}
