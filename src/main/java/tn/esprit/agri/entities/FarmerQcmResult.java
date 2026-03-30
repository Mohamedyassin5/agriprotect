package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "farmer_qcm_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmerQcmResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private QcmTest test;

    @Column(nullable = false)
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private boolean passed;

    @Column
    private LocalDate discountValidUntil;
}
