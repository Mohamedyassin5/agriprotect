package tn.esprit.agri.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import tn.esprit.agri.entities.enums.StatutSinistre;
import tn.esprit.agri.entities.enums.TypeSinistre;

import java.time.LocalDateTime;

@Entity
@Table(name = "declarations_sinistres", indexes = {
        @Index(name = "idx_decl_sinistre_crop", columnList = "crop_id"),
        @Index(name = "idx_decl_sinistre_user", columnList = "user_id"),
        @Index(name = "idx_decl_sinistre_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sinistre {

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

    @Column(name = "date_catastrophe", nullable = false)
    private LocalDateTime dateCatastrophe;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_sinistre", nullable = false)
    private TypeSinistre typeSinistre;

    @Column(name = "quota_remboursement")
    private Float quotaRemboursement; // Pourcentage

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutSinistre statut = StatutSinistre.EN_ATTENTE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "is_resolved", nullable = false)
    @Builder.Default
    private Boolean isResolved = false;
}
