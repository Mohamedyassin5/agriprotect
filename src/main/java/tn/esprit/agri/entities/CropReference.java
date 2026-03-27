package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crop_references")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropReference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String cropType;  // ex: "olives", "cereales"

    @Column(nullable = false)
    private Integer referenceYear;  // ex: 2026

    @Column(nullable = false)
    private Float referenceYield;  // t/ha

    @Column(nullable = false)
    private Float referencePrice;  // TND/t

    @Column(nullable = false)
    private Float basePremiumRate;  // ex: 0.055 (5.5%)
}