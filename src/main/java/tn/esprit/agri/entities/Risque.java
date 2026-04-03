package tn.esprit.agri.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import tn.esprit.agri.entities.enums.RiskType;
import tn.esprit.agri.entities.enums.Severity;

import java.time.LocalDateTime;

@Entity
@Table(name = "risques", indexes = {
        @Index(name = "idx_risque_crop", columnList = "crop_id"),
        @Index(name = "idx_risque_user", columnList = "user_id"),
        @Index(name = "idx_risque_resolved", columnList = "is_resolved")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Risque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", nullable = false)
    @JsonBackReference
    private Crop crop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskType typeSinistre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "current_value")
    private Float currentValue;

    @Column(name = "max_allowed")
    private Float maxAllowed;

    @Column(name = "min_allowed")
    private Float minAllowed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved = false;
}

