package tn.esprit.agri.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.agri.dto_savings_accountability.BudgetRequest;
import tn.esprit.agri.dto_savings_accountability.BudgetResponse;
import tn.esprit.agri.entities.enums.BudgetPeriodType;
import tn.esprit.agri.entities.enums.EntryCategory;

import java.time.LocalDate;

public interface IBudgetService {
    BudgetResponse create(String userId, BudgetRequest request);
    Page<BudgetResponse> getBudgets(String userId, BudgetPeriodType periodType, EntryCategory category,
                                    LocalDate startDate, LocalDate endDate, Pageable pageable);
    BudgetResponse getById(Long id, String userId);
    BudgetResponse update(Long id, String userId, BudgetRequest request);
    void delete(Long id, String userId);
}
