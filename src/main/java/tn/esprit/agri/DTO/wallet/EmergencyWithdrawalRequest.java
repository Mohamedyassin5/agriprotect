package tn.esprit.agri.DTO.wallet;

import lombok.Data;
import tn.esprit.agri.entities.enums.EmergencyWithdrawalReason;

import java.math.BigDecimal;

@Data
public class EmergencyWithdrawalRequest {
    private BigDecimal amount;
    private EmergencyWithdrawalReason reason;
    private String description;
    private String supportingDocumentUrl;
}
