package tn.esprit.agri.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "savings_goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "savings_account_id", nullable = false)
    @JsonBackReference
    private SavingsAccount savingsAccount;

    @Column(nullable = false, length = 200)
    private String goalName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate targetDate;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean achieved = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean collected = false;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    /**
     * Priorité de financement : 1 = financé en premier, 2 = deuxième, etc.
     * 0 = pas de priorité spécifique (distribution proportionnelle).
     * Les objectifs avec priorité > 0 sont financés séquentiellement avant les autres.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    /**
     * Pourcentage personnalisé d'allocation du solde (0-100).
     * Si renseigné, ce pourcentage du solde total du compte est réservé à cet objectif.
     * Si null, l'allocation est automatique (proportionnelle ou par priorité).
     */
    @Column(name = "custom_allocation_percentage", precision = 5, scale = 2)
    private BigDecimal customAllocationPercentage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
