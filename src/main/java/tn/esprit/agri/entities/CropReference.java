package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crop_references",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cropType", "referenceYear"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropReference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String cropType;

    @Column(nullable = false)
    private Integer referenceYear;

    @Column(nullable = false)
    private Float referenceYield;      // tonnes/ha  → suppression de precision/scale

    @Column(nullable = false)
    private Float referencePrice;      // TND/tonne  → suppression de precision/scale

    @Column(nullable = false)
    private Float basePremiumRate;     // ex: 0.012

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}