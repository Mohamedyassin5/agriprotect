package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tn.esprit.agri.entities.enums.InvestigationStatus;
import tn.esprit.agri.entities.enums.InvestigationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "investigations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investigation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indemnisation_request_id", nullable = false)
    private IndemnisationRequest indemnisationRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id", nullable = false)
    private User assignedExpert;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestigationType type;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestigationStatus status = InvestigationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String decisionReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
