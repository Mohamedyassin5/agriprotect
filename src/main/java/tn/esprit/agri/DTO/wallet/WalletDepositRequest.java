package tn.esprit.agri.DTO.wallet;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WalletDepositRequest {
    private BigDecimal amount;
    private String description;
}
