package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import tn.esprit.agri.controlleurs.auth.dto.ChangePasswordRequest;
import tn.esprit.agri.controlleurs.user.dto.RegisterRequest;
import tn.esprit.agri.controlleurs.user.dto.UpdateUserRequest;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.services.IUserService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/agri/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final IUserService userService;


    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody RegisterRequest req) {
        try {
            if (userService.emailExists(req.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
            }

            User user = User.builder()
                    .email(req.getEmail())
                    .password(req.getPassword())
                    .firstName(req.getFirstName())
                    .lastName(req.getLastName())
                    .phoneNumber(req.getPhoneNumber())
                    .address(req.getAddress())
                    .role(req.getRole())
                    .expertFundId(req.getExpertFundId())
                    .build();

            User createdUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error");
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }


    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @Valid @RequestBody UpdateUserRequest req) {
        try {
            User userDetails = new User();
            userDetails.setFirstName(req.getFirstName());
            userDetails.setLastName(req.getLastName());
            userDetails.setPhoneNumber(req.getPhoneNumber());
            userDetails.setAddress(req.getAddress());
            userDetails.setScore(req.getScore());

            Optional<User> updatedUser = userService.updateUser(id, userDetails);
            return updatedUser.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        try {
            boolean deleted = userService.deleteUser(id);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting user: " + e.getMessage() + " | Cause: " + 
                          (e.getCause() != null ? e.getCause().getMessage() : "none"));
        }
    }


    @GetMapping("/email/{email}/exists")
    public ResponseEntity<Boolean> checkEmailExists(@PathVariable String email) {
        boolean exists = userService.emailExists(email);
        return ResponseEntity.ok(exists);
    }


    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String keyword) {
        List<User> users = userService.searchByName(keyword);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changeMyPassword(@Valid @RequestBody ChangePasswordRequest request,
                                              Authentication authentication) {
        try {
            // 1) confirm new password
            if (request.getNewPassword() == null || request.getConfirmNewPassword() == null
                    || !request.getNewPassword().equals(request.getConfirmNewPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Passwords do not match");
            }

            String email = authentication.getName(); // from JWT subject
            userService.changePassword(email, request.getOldPassword(), request.getNewPassword());

            return ResponseEntity.ok("Password changed successfully. Please login again.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error");
        }
    }

    /**
     * Get current authenticated user profile
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Get current authenticated user balance only
     */
    @GetMapping("/me/balance")
    public ResponseEntity<Double> getMyBalance(Authentication authentication) {
        if (authentication == null) return ResponseEntity.ok(0.0);
        String email = authentication.getName();
        return ResponseEntity.ok(userService.getUserByEmail(email)
                .map(User::getAccountBalance)
                .orElse(0.0));
    }
}
