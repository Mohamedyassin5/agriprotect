package tn.esprit.agri.DTO.wallet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmergencyFundTopUpRequest {
    private BigDecimal amount;
}
