package tn.esprit.agri.DTO.CreditDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreditSimulationRequestDto {
    @NotEmpty
    @Valid
    private List<CreditSimulationOfferDto> offres;

    private String criteria = "MIN_TOTAL_COST";
}
