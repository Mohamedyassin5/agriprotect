package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.agri.entities.enums.StatutDemande;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dateDemande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDemande statut;

    @Column(nullable = false)
    private Long agriculteurId;

    private Double montantDemande;

    @Column(length = 500)
    private String description;

    @OneToOne(mappedBy = "demandeCredit", cascade = CascadeType.ALL, orphanRemoval = true)
    private AnalyseRentabilite analyseRentabilite;

    @OneToOne(mappedBy = "demandeCredit", fetch = FetchType.LAZY)
    private Credit credit;

    private LocalDateTime instructionAt;
    private LocalDateTime decisionAt;
    private Long updatedBy;

}