package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.dto_savings_accountability.BudgetRequest;
import tn.esprit.agri.dto_savings_accountability.BudgetResponse;
import tn.esprit.agri.entities.Budget;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.BudgetPeriodType;
import tn.esprit.agri.entities.enums.EntryCategory;
import tn.esprit.agri.repositories.BudgetRepository;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.IBudgetService;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class BudgetServiceImpl implements IBudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    @Override
    public BudgetResponse create(String userId, BudgetRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (budgetRepository.existsByUserIdAndCategoryAndPeriodStartAndPeriodEnd(
                userId, request.getCategory(), request.getPeriodStart(), request.getPeriodEnd())) {
            throw new IllegalArgumentException(
                    "Un budget pour la catégorie '" + request.getCategory() +
                            "' existe déjà pour cette période (" + request.getPeriodStart() + " → " + request.getPeriodEnd() + ")");
        }

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setPeriodType(request.getPeriodType());
        budget.setPeriodStart(request.getPeriodStart());
        budget.setPeriodEnd(request.getPeriodEnd());
        budget.setCategory(request.getCategory());
        budget.setPlannedAmount(request.getPlannedAmount());

        Budget saved = budgetRepository.save(budget);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BudgetResponse> getBudgets(String userId, BudgetPeriodType periodType, EntryCategory category,
                                           LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Budget> budgets = budgetRepository.findByFilters(userId, periodType, category, startDate, endDate, pageable);
        return budgets.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getById(Long id, String userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Budget not found or access denied"));
        return toResponse(budget);
    }

    @Override
    public BudgetResponse update(Long id, String userId, BudgetRequest request) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Budget not found or access denied"));

        if (request.getPeriodType() != null) budget.setPeriodType(request.getPeriodType());
        if (request.getPeriodStart() != null) budget.setPeriodStart(request.getPeriodStart());
        if (request.getPeriodEnd() != null) budget.setPeriodEnd(request.getPeriodEnd());
        if (request.getCategory() != null) budget.setCategory(request.getCategory());
        if (request.getPlannedAmount() != null) budget.setPlannedAmount(request.getPlannedAmount());

        Budget updated = budgetRepository.save(budget);
        return toResponse(updated);
    }

    @Override
    public void delete(Long id, String userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Budget not found or access denied"));
        budgetRepository.delete(budget);
    }

    private BudgetResponse toResponse(Budget budget) {
        BudgetResponse response = new BudgetResponse();
        response.setId(budget.getId());
        response.setPeriodType(budget.getPeriodType());
        response.setPeriodStart(budget.getPeriodStart());
        response.setPeriodEnd(budget.getPeriodEnd());
        response.setCategory(budget.getCategory());
        response.setPlannedAmount(budget.getPlannedAmount());
        response.setCreatedAt(budget.getCreatedAt());
        response.setUpdatedAt(budget.getUpdatedAt());
        return response;
    }
}
