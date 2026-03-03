package tn.esprit.agri.DTO.CreditDTO;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class CreationCreditDto {

    @NotNull @Positive
    private Double montant;

    @NotNull @Positive @DecimalMin("0.1")
    private Double tauxInteret;

    @NotNull @Min(1)
    private Integer dureeMois;

    @NotNull @FutureOrPresent
    private LocalDate dateDebut;

    private Long assuranceId;

}