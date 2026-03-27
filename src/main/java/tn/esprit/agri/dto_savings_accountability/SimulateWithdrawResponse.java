package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulateWithdrawResponse {

    private BigDecimal currentBalance;
    private BigDecimal withdrawalAmount;
    private BigDecimal balanceAfterWithdrawal;
    private Boolean isAllowed;
    private String message;
    private BigDecimal goalAmount;
    private BigDecimal goalImpact;
}
