package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.agri.entities.enums.StatutEcheance;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Echeance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dateEcheance;

    @Column(nullable = false)
    private Double montantDu; 

    @Column(nullable = false)
    private Double montantPaye = 0.0; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEcheance statut;

    private Double capitalDu;
    private Double interetsDu;
    private Double assuranceDu;

    private LocalDate datePaiementEffectif; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_id", nullable = false)
    private Credit credit;

    private Integer numeroEcheance;
}