package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.agri.entities.enums.SavingsAccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsAccountResponse {

    private Long id;
    private String userId;
    private String accountName;
    private BigDecimal currentBalance;
    private BigDecimal monthlySavingsTarget;
    private BigDecimal goalAmount;
    private String goalTitle;
    private SavingsAccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
