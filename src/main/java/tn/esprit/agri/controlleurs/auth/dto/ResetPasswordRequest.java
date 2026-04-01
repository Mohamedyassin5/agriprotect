package tn.esprit.agri.controlleurs.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._#-])[A-Za-z\\d@$!%*?&._#-]{8,}$",
            message = "Password must be 8+ chars with upper, lower, digit, and special (@$!%*?&._#-)"
    )

    private String newPassword;

    @NotBlank
    private String confirmNewPassword;
}
