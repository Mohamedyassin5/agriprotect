package tn.esprit.agri.DTO;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EcheancePaiementDto {

    @PositiveOrZero
    private Double montantPaye;

    private LocalDate datePaiement; 

    private String referencePaiement;
}