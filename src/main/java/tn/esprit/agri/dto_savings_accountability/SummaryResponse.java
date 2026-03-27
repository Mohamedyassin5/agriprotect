package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryResponse {

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netIncome;
    private Integer incomeTransactionCount;
    private Integer expenseTransactionCount;
}
