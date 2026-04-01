package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.agri.entities.enums.BudgetPeriodType;
import tn.esprit.agri.entities.enums.EntryCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetResponse {

    private Long id;
    private String userId;
    private BudgetPeriodType periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private EntryCategory category;
    private BigDecimal plannedAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
