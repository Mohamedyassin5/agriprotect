package tn.esprit.agri.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qcm_tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QcmTest {

    @Id
    private String id;

    private String title;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Builder.Default
    private double requiredScore = 0.0;


    // Kept to satisfy DB constraint "Field 'discount_percentage' doesn't have a
    // default value"
    @Builder.Default
    @Column(name = "discount_percentage", nullable = false)
    private double discountPercentage = 10.0;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_id", referencedColumnName = "id")
    @com.fasterxml.jackson.annotation.JsonBackReference
    private SolidarityFund fund;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<QcmQuestion> questions = new ArrayList<>();
}
