package tn.esprit.agri.controlleurs.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(min = 2, max = 40, message = "First name must be 2-40 chars")
    private String firstName;

    @Size(min = 2, max = 40, message = "Last name must be 2-40 chars")
    private String lastName;

    private String phoneNumber;
    private String address;
    private Float score;
}
