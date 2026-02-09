package tn.esprit.agri.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Crop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String cropType;
    private Float surface;
    private Float optimalHumidity;
    private Float minHumidity;
    private Float maxHumidity;
    private Float minTemperature;
    private Float maxTemperature;
    private Float averageTemperature;
    private LocalDate startDate;
    private LocalDate endDate;
    private String typeterres;
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;
}
