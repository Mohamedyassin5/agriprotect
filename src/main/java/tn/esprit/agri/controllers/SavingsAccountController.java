package tn.esprit.agri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.dto_savings_accountability.*;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.services.ISavingsAccountService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/savings/accounts")
@RequiredArgsConstructor
public class SavingsAccountController {

    private final ISavingsAccountService savingsAccountService;

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<SavingsAccountResponse> createAccount(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SavingsAccountRequest request) {
        SavingsAccountResponse response = savingsAccountService.create(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<SavingsAccountResponse> getMyAccount(@AuthenticationPrincipal User user) {
        SavingsAccountResponse response = savingsAccountService.getMyAccount(user.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<SavingsAccountResponse> updateMyAccount(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SavingsAccountRequest request) {
        SavingsAccountResponse response = savingsAccountService.update(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<Void> closeMyAccount(@AuthenticationPrincipal User user) {
        savingsAccountService.delete(user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/goal/progress")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<GoalProgressResponse> getGoalProgress(@AuthenticationPrincipal User user) {
        GoalProgressResponse response = savingsAccountService.getGoalProgress(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary/monthly")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "6") int months) {
        MonthlySummaryResponse summary = savingsAccountService.getMonthlySummary(user.getId(), months);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<AlertsResponse> getAlerts(@AuthenticationPrincipal User user) {
        AlertsResponse alerts = savingsAccountService.getAlerts(user.getId());
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/simulate/withdraw")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<SimulateWithdrawResponse> simulateWithdraw(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, BigDecimal> request) {
        BigDecimal amount = request.get("amount");
        SimulateWithdrawResponse response = savingsAccountService.simulateWithdraw(user.getId(), amount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendation")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<RecommendationResponse> getRecommendation(@AuthenticationPrincipal User user) {
        RecommendationResponse recommendation = savingsAccountService.getRecommendation(user.getId());
        return ResponseEntity.ok(recommendation);
    }

    @GetMapping("/statement")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<StatementResponse> getStatement(
            @AuthenticationPrincipal User user,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        StatementResponse statement = savingsAccountService.getStatement(user.getId(), from, to);
        return ResponseEntity.ok(statement);
    }
}
