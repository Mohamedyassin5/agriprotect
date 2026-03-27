package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.agri.entities.enums.SavingsTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long id;
    private Long accountId;
    private SavingsTransactionType type;
    private BigDecimal amount;
    private String description;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
}
