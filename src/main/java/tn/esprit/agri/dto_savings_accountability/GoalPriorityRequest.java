package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalPriorityRequest {

    /**
     * Priorité de financement.
     * 1 = financé en premier (reçoit le solde disponible avant les autres).
     * 2 = deuxième, 3 = troisième, etc.
     * 0 = aucune priorité (distribution automatique proportionnelle au montant cible).
     * Deux objectifs peuvent avoir la même priorité.
     */
    private Integer priority;

    /**
     * Pourcentage personnalisé du solde du compte à allouer à cet objectif (0.00 - 100.00).
     * Exemple : 50.00 signifie que 50% du solde total ira toujours à cet objectif.
     * null ou non renseigné = allocation automatique (par priorité ou proportionnelle).
     * ATTENTION : la somme de tous les customAllocationPercentage ne peut pas dépasser 100%.
     */
    private BigDecimal customAllocationPercentage;

    /**
     * Si true, remet l'allocation à automatique (efface customAllocationPercentage).
     */
    private boolean resetToAuto = false;
}
