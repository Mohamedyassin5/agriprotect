package tn.esprit.agri.DTO;

import lombok.Data;
import tn.esprit.agri.entities.enums.CoverageType;
import tn.esprit.agri.entities.enums.InsuranceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InsuranceResponse {
    private String id;
    private String policyNumber;
    private CoverageType coverageType;
    private BigDecimal insuredAmount;
    private BigDecimal premiumAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private InsuranceStatus status;
    private String message; // ex: "Police créée avec succès"
}