package tn.esprit.agri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.dto_savings_accountability.BudgetRequest;
import tn.esprit.agri.dto_savings_accountability.BudgetResponse;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.BudgetPeriodType;
import tn.esprit.agri.entities.enums.EntryCategory;
import tn.esprit.agri.services.IBudgetService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/accounting/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final IBudgetService budgetService;

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<BudgetResponse> createBudget(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BudgetRequest request) {

        BudgetResponse response = budgetService.create(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<Page<BudgetResponse>> listBudgets(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) BudgetPeriodType periodType,
            @RequestParam(required = false) EntryCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("periodStart").descending());
        Page<BudgetResponse> response = budgetService.getBudgets(user.getId(), periodType, category, from, to, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<BudgetResponse> getBudget(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        BudgetResponse response = budgetService.getById(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<BudgetResponse> updateBudget(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {

        BudgetResponse response = budgetService.update(id, user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<Void> deleteBudget(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        budgetService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
