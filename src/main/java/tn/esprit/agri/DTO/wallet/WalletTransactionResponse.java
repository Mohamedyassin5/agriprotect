package tn.esprit.agri.DTO.wallet;

import lombok.Builder;
import lombok.Data;
import tn.esprit.agri.entities.enums.WalletTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class WalletTransactionResponse {
    private Long id;
    private WalletTransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private Long referenceId;
    private String referenceType;
    private LocalDateTime createdAt;
}
