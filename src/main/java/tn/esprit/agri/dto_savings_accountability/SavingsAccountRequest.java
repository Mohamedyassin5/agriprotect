package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.agri.entities.enums.SavingsAccountStatus;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsAccountRequest {

    private String accountName;

    private BigDecimal monthlySavingsTarget;

    private SavingsAccountStatus status;
}
