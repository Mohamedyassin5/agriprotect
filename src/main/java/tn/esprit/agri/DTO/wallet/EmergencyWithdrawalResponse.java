package tn.esprit.agri.DTO.wallet;

import lombok.Builder;
import lombok.Data;
import tn.esprit.agri.entities.enums.EmergencyWithdrawalReason;
import tn.esprit.agri.entities.enums.EmergencyWithdrawalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class EmergencyWithdrawalResponse {
    private Long id;
    private String walletUserId;
    private String walletUserName;
    private String walletUserEmail;
    private BigDecimal amount;
    private EmergencyWithdrawalReason reason;
    private String description;
    private EmergencyWithdrawalStatus status;
    private String adminNote;
    private LocalDateTime createdAt;
}
