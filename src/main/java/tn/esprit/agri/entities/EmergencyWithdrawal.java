package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tn.esprit.agri.entities.enums.EmergencyWithdrawalReason;
import tn.esprit.agri.entities.enums.EmergencyWithdrawalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_withdrawal")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EmergencyWithdrawal {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmergencyWithdrawalReason reason;

    @Column(length = 1000)
    private String description;

    @Column(name = "supporting_document_url", length = 500)
    private String supportingDocumentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EmergencyWithdrawalStatus status = EmergencyWithdrawalStatus.PENDING_APPROVAL;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Column(name = "approved_by")
    private String approvedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
