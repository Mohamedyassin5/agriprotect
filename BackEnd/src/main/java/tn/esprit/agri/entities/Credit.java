package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.agri.entities.enums.StatutCredit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Credit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double montant;    

    @Column(nullable = false)
    private Double tauxInteret;    

    @Column(nullable = false)
    private Integer dureeMois;    

    @Column(nullable = false)
    private LocalDate dateDebut;   

    @Column(nullable = false)
    private LocalDate dateFin;      

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCredit statut;

    @Column(nullable = false)
    private Long agriculteurId;  

    @Column
    private Long assuranceId;   

    @OneToOne
    @JoinColumn(name = "demande_credit_id", nullable = false, unique = true)
    private DemandeCredit demandeCredit;

    private Double montantRembourse;
    private LocalDate derniereEcheancePayee;
    private String referenceContrat; 

    @OneToMany(mappedBy = "credit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Echeance> echeances = new ArrayList<>();

}