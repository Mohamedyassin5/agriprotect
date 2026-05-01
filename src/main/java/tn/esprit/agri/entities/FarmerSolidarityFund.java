package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.agri.entities.enums.MembershipStatus;

import java.time.LocalDate;

@Entity
@Table(name = "farmer_solidarity_funds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmerSolidarityFund {

    @EmbeddedId
    private FarmerSolidarityFundId id;

    @MapsId("farmerId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @MapsId("solidarityFundId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_id", nullable = false)
    private SolidarityFund solidarityFund;

    @Column(nullable = false)
    private LocalDate dateAdhesion;

    @Builder.Default
    @Column(nullable = false)
    private Integer monthsPaid = 0;

    @Builder.Default
    @Column(nullable = false)
    private Double totalPaid = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Double discountPercentage = 0.0;

    @Column
    private Double currentPrimeAmount;

    @Column
    private LocalDate lastPaymentDate;

    @Column
    private LocalDate lastIndemnisationDate;
}
