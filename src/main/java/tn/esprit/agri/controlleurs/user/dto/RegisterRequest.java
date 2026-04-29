package tn.esprit.agri.controlleurs.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @Email(message = "Email invalid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._#-])[A-Za-z\\d@$!%*?&._#-]{8,}$",
            message = "Password must be 8+ chars with upper, lower, digit, and special (@$!%*?&._#-)"
    )

    private String password;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 40, message = "First name must be 2-40 chars")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 40, message = "Last name must be 2-40 chars")
    private String lastName;

    private String phoneNumber;
    private String address;
    private tn.esprit.agri.entities.enums.Role role;
    private String expertFundId;
}
