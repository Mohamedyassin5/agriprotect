package tn.esprit.agri.DTO;

import lombok.Data;

@Data
public class SignRequestDTO {
    private String signatureName;  // ex: "Ahmed Ben Jannet"
    private boolean acceptTerms = true;  // Checkbox "J'accepte les conditions"
}