package tn.esprit.agri.DTO.EcheanceDTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EcheancePaiementDto {

    @NotNull
    @PositiveOrZero
    private Double montantPaye;

    private LocalDate datePaiement; 

    private String referencePaiement;
}