package tn.esprit.agri.DTO.CreditDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PortfolioAlertDto {
    private String code;
    private String message;
}
