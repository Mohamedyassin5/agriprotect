package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementResponse {

    private Long accountId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private List<TransactionResponse> transactions;
}
