package tn.esprit.agri.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.agri.dto_savings_accountability.*;
import tn.esprit.agri.entities.enums.EntryCategory;
import tn.esprit.agri.entities.enums.EntryType;

import java.time.LocalDate;

public interface IAccountingEntryService {
    // CRUD operations
    EntryResponse create(String userId, EntryRequest request);
    Page<EntryResponse> getEntries(String userId, EntryType type, EntryCategory category,
                                   LocalDate from, LocalDate to, Pageable pageable);
    EntryResponse getById(Long id, String userId);
    EntryResponse update(Long id, String userId, EntryRequest request);
    void delete(Long id, String userId);

    // Analytics operations
    SummaryResponse getSummary(String userId, LocalDate from, LocalDate to);
    BudgetVsActualResponse getBudgetVsActual(String userId, LocalDate periodStart, LocalDate periodEnd);
    SpendingBreakdownResponse getSpendingBreakdown(String userId, LocalDate from, LocalDate to);
    CashflowForecastResponse getCashflowForecast(String userId);
    OverspendingAlertResponse getOverspendingAlerts(String userId, LocalDate periodStart, LocalDate periodEnd);
    AnomalyResponse getAnomalies(String userId, LocalDate from, LocalDate to);
}
