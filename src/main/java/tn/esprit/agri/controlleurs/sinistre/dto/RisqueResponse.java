package tn.esprit.agri.controlleurs.sinistre.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.agri.entities.enums.RiskType;
import tn.esprit.agri.entities.enums.Severity;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RisqueResponse {

    private String id;
    private String cropId;
    private String cropType;
    private String userId;
    private String userEmail;
    private RiskType riskType;
    private String description;
    private Float currentValue;
    private Float maxAllowed;
    private Float minAllowed;
    private Severity severity;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private Boolean isResolved;
}
