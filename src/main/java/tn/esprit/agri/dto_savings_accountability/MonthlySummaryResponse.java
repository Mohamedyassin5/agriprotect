package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySummaryResponse {

    private String period;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal netChange;
    private BigDecimal startBalance;
    private BigDecimal endBalance;
    private Integer transactionCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyData {
        private String month;
        private BigDecimal deposits;
        private BigDecimal withdrawals;
        private BigDecimal netChange;
        private BigDecimal balance;
    }

    private List<MonthlyData> monthlyData;
}
