package tn.esprit.agri.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import tn.esprit.agri.entities.enums.SavingsTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "savings_transaction", indexes = {
        @Index(name = "idx_savings_tx_account_date", columnList = "account_id, occurred_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonBackReference("account-transactions")
    private SavingsAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SavingsTransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
