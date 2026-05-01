package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.agri.entities.enums.FundStatus;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "solidarity_funds", uniqueConstraints = @UniqueConstraint(columnNames = { "name", "cultureType" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolidarityFund {

    @Id
    @Column(nullable = false, updatable = false)
    private String id; // Will use "name-cultureType" as ID

    @Column(nullable = false, unique = true)
    private String numeroFond;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String cultureType;

    @Builder.Default
    @Column(nullable = false)
    private Integer minScore = 40; // Automatically set for all new funds

    @Column(nullable = false)
    private Double primeAmount;

    @Builder.Default
    @Column(nullable = false)
    private Double currentBalance = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime creationDate = LocalDateTime.now();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FundStatus status = FundStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "solidarityFund", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<FarmerSolidarityFund> members = new HashSet<>();

    @OneToOne(mappedBy = "fund", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private QcmTest test;
}
