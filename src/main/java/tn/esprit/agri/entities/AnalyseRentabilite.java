package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.agri.entities.enums.DecisionCredit;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseRentabilite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double revenuBrut;     

    @Column(nullable = false)
    private Double coutTotal;  

    @Column(nullable = false)
    private Double beneficeNet; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionCredit decision;

    private String commentaire;

    private LocalDateTime dateAnalyse;

    @OneToOne
    @JoinColumn(name = "demande_credit_id", nullable = false)
    private DemandeCredit demandeCredit;

    private String analysteId;

    private Double scoreRevenueStability;
    private Double scoreDebtRatio;
    private Double scoreProjectRisk;
    private Double scoreHistory;
    private Double scoreFinal;
    private String recommendation;
}