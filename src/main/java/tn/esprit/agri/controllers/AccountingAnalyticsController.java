package tn.esprit.agri.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.dto_savings_accountability.*;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.services.IAccountingEntryService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
public class AccountingAnalyticsController {

    private final IAccountingEntryService entryService;

    // A1: Summary
    @GetMapping("/summary")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<SummaryResponse> getSummary(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        SummaryResponse response = entryService.getSummary(user.getId(), from, to);
        return ResponseEntity.ok(response);
    }

    // A2: Budget vs Actual
    @GetMapping("/budget-vs-actual")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<BudgetVsActualResponse> getBudgetVsActual(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {

        BudgetVsActualResponse response = entryService.getBudgetVsActual(user.getId(), periodStart, periodEnd);
        return ResponseEntity.ok(response);
    }

    // A3: Spending Breakdown
    @GetMapping("/spending-breakdown")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<SpendingBreakdownResponse> getSpendingBreakdown(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        SpendingBreakdownResponse response = entryService.getSpendingBreakdown(user.getId(), from, to);
        return ResponseEntity.ok(response);
    }

    // A4: Cashflow Forecast
    @GetMapping("/cashflow/forecast")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<CashflowForecastResponse> getCashflowForecast(@AuthenticationPrincipal User user) {
        CashflowForecastResponse response = entryService.getCashflowForecast(user.getId());
        return ResponseEntity.ok(response);
    }

    // A5: Overspending Alerts
    @GetMapping("/alerts/overspending")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<OverspendingAlertResponse> getOverspendingAlerts(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {

        OverspendingAlertResponse response = entryService.getOverspendingAlerts(user.getId(), periodStart, periodEnd);
        return ResponseEntity.ok(response);
    }

    // A6: Anomalies
    @GetMapping("/anomalies")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<AnomalyResponse> getAnomalies(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        AnomalyResponse response = entryService.getAnomalies(user.getId(), from, to);
        return ResponseEntity.ok(response);
    }
}
