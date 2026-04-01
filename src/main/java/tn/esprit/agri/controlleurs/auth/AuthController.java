package tn.esprit.agri.controlleurs.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.agri.ai.client.FaceRecognitionClient;
import tn.esprit.agri.ai.dto.FaceMatchResponse;
import tn.esprit.agri.controlleurs.auth.dto.ForgotPasswordRequest;
import tn.esprit.agri.controlleurs.auth.dto.LoginRequest;
import tn.esprit.agri.controlleurs.auth.dto.LoginResponse;
import tn.esprit.agri.controlleurs.auth.dto.ResetPasswordRequest;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.security.JwtService;
import tn.esprit.agri.services.AuthPasswordResetService;
import tn.esprit.agri.utils.InMemoryMultipartFile;

@RestController
@RequestMapping("/agri/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthPasswordResetService resetService;

    // Face recognition dependencies
    private final UserRepository userRepository;
    private final FaceRecognitionClient faceClient;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        String token = jwtService.generateToken(auth.getName());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        resetService.forgotPassword(req.getEmail());
        return ResponseEntity.ok("Si l'email existe, un code a été envoyé.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest req) {
        resetService.resetPassword(req);
        return ResponseEntity.ok("Mot de passe modifié avec succès.");
    }

    // ✅ FACE ENROLL (protected)
    @PostMapping(value = "/face/enroll", consumes = "multipart/form-data")
    public ResponseEntity<?> faceEnroll(@RequestPart("image") MultipartFile image,
                                        Authentication authentication) throws Exception {
        if (authentication == null) return ResponseEntity.status(401).body("Unauthorized");
        if (image == null || image.isEmpty()) return ResponseEntity.badRequest().body("Image is required");

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFaceRefImage(image.getBytes());
        user.setFaceEnabled(true);
        userRepository.save(user);

        return ResponseEntity.ok("Face enrolled");
    }

    // ✅ FACE LOGIN (public) -> generates JWT
    @PostMapping(value = "/face/login", consumes = "multipart/form-data")
    public ResponseEntity<LoginResponse> faceLogin(@RequestParam("email") String email,
                                                   @RequestPart("image") MultipartFile liveImage) {
        if (liveImage == null || liveImage.isEmpty())
            throw new RuntimeException("Image is required");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.isFaceEnabled() || user.getFaceRefImage() == null)
            throw new RuntimeException("Face not enrolled");

        // create a fake MultipartFile for ref image (stored in DB)
        MultipartFile refImage = new InMemoryMultipartFile("ref.jpg", user.getFaceRefImage());

        FaceMatchResponse result = faceClient.compare(refImage, liveImage);

        if (result.getError() != null) {
            throw new RuntimeException(result.getError());
        }

        if (!result.isMatch()) {
            throw new RuntimeException("Face not matched (distance=" + result.getDistance() + ")");
        }

        String token = jwtService.generateToken(user.getEmail());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
