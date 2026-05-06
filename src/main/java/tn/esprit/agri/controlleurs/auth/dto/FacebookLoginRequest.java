package tn.esprit.agri.controlleurs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacebookLoginRequest {
    private String accessToken;
}
