package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "indemnisation_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndemnisationRequest {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_id", nullable = false)
    private SolidarityFund fund;

    @Column(nullable = false)
    private Double requestedAmount;

    @Column(nullable = false)
    private LocalDateTime requestDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(columnDefinition = "TEXT")
    private String requestReason;

    @Column(columnDefinition = "TEXT")
    private String refusalReason;

    private LocalDateTime processedDate;

    @Column
    private Double aiScore;

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;

    @Column
    private String imageProofUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REFUSED
    }
}
