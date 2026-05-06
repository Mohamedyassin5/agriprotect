package tn.esprit.agri.DTO.wallet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletTransferRequest {
    private String recipientUserId;
    private String recipientEmail;
    private BigDecimal amount;
    private String description;
}
