package tn.esprit.agri.dto_savings_accountability;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.agri.entities.enums.BudgetPeriodType;
import tn.esprit.agri.entities.enums.EntryCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetRequest {

    @NotNull(message = "Period type is required")
    private BudgetPeriodType periodType;

    @NotNull(message = "Period start date is required")
    private LocalDate periodStart;

    @NotNull(message = "Period end date is required")
    private LocalDate periodEnd;

    @NotNull(message = "Category is required")
    private EntryCategory category;

    @NotNull(message = "Planned amount is required")
    @DecimalMin(value = "0.01", message = "Planned amount must be greater than 0")
    private BigDecimal plannedAmount;
}
