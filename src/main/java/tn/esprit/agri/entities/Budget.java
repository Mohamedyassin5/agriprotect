package tn.esprit.agri.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tn.esprit.agri.entities.enums.BudgetPeriodType;
import tn.esprit.agri.entities.enums.EntryCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget",
        indexes = {
                @Index(name = "idx_budget_user_period", columnList = "user_id, period_start, period_end")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_budget_user_category_period",
                        columnNames = {"user_id", "category", "period_start", "period_end"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-budgets")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private BudgetPeriodType periodType;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryCategory category;

    @Column(name = "planned_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal plannedAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
