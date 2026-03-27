package tn.esprit.agri.DTO.AnalyseDTO;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.agri.entities.enums.DecisionCredit;

import java.time.LocalDateTime;

@Getter
@Builder
public class AnalyseRentabiliteResponseDto {

    private Long id;
    private Double revenuBrut;
    private Double coutTotal;
    private Double beneficeNet;
    private DecisionCredit decision;
    private String commentaire;
    private LocalDateTime dateAnalyse;
    private Long demandeCreditId;
    private Long analysteId;
}