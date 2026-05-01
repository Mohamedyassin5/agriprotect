package tn.esprit.agri.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SolidarityFundResponse {

    private String id;
    private String name;
    private String numeroFond;
    private String cultureType;
    private Integer minScore;
    private Double primeAmount;
    private Double currentBalance;
    private String createdByUsername;
}
