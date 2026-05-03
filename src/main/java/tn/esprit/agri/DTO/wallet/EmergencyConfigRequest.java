package tn.esprit.agri.DTO.wallet;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EmergencyConfigRequest {
    private BigDecimal targetAmount;
    private BigDecimal monthlyContribution;
    private boolean autoContribute;
    private Integer contributionDay;
}
