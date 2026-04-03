package tn.esprit.agri.controlleurs.sinistre.dto;

import lombok.Data;
import tn.esprit.agri.entities.enums.StatutSinistre;
import tn.esprit.agri.entities.enums.TypeSinistre;

import java.time.LocalDateTime;

@Data
public class SinistreResponse {
    private String id;
    private String cropId;
    private String cropType;
    private String userId;
    private String userEmail;
    private LocalDateTime dateCatastrophe;
    private String imageUrl;
    private TypeSinistre typeSinistre;
    private Float quotaRemboursement;
    private String description;
    private StatutSinistre statut;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private Boolean isResolved;
}
