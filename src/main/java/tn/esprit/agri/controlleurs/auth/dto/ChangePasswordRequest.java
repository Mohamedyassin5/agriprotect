package tn.esprit.agri.controlleurs.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._#-])[A-Za-z\\d@$!%*?&._#-]{8,}$",
        message = "Password must be 8+ chars with upper, lower, digit, and special (@$!%*?&._#-)"
    )
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmNewPassword;
}
