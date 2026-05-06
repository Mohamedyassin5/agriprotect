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
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import tn.esprit.agri.controlleurs.auth.dto.GoogleLoginRequest;
import tn.esprit.agri.controlleurs.auth.dto.FacebookLoginRequest;
import tn.esprit.agri.entities.enums.Role;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

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
        String role = auth.getAuthorities().isEmpty() ? "FARMER" : auth.getAuthorities().iterator().next().getAuthority();
        System.out.println("====== SUPER DEBUG: USER ROLE EXTRACTED BY BACKEND = " + role + " ======");

        // Fetch user to get name claims
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(auth.getName(), role, user.getFirstName(), user.getLastName(), user.getId());
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

        String token = jwtService.generateToken(user.getEmail(), "ROLE_" + user.getRole().name(), user.getFirstName(), user.getLastName(), user.getId());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@RequestBody GoogleLoginRequest request) throws Exception {
        NetHttpTransport transport = new NetHttpTransport();
        GsonFactory gsonFactory = GsonFactory.getDefaultInstance();

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, gsonFactory)
                .setAudience(Collections.singletonList("912952078302-4v1btjbicaoflhfuhfdhpmkb8st3b5mo.apps.googleusercontent.com"))
                .build();

        GoogleIdToken idToken = verifier.verify(request.getIdToken());
        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                // Register new user
                User newUser = User.builder()
                        .email(email)
                        .firstName((String) payload.get("given_name"))
                        .lastName((String) payload.get("family_name"))
                        .role(Role.FARMER)
                        .password(UUID.randomUUID().toString()) // Dummy password for Google users
                        .score(50.0f)
                        .faceEnabled(false)
                        .status(tn.esprit.agri.entities.enums.Status.ACTIVE)
                        .build();
                return userRepository.save(newUser);
            });

            String token = jwtService.generateToken(user.getEmail(), "ROLE_" + user.getRole().name(), user.getFirstName(), user.getLastName(), user.getId());
            return ResponseEntity.ok(new LoginResponse(token));
        } else {
            throw new RuntimeException("Invalid Google ID token.");
        }
    }

    @PostMapping("/facebook")
    public ResponseEntity<LoginResponse> facebookLogin(@RequestBody FacebookLoginRequest request) {
        String url = "https://graph.facebook.com/me?fields=id,first_name,last_name,email&access_token=" + request.getAccessToken();
        RestTemplate restTemplate = new RestTemplate();

        @SuppressWarnings("unchecked")
        Map<String, Object> fbUser = restTemplate.getForObject(url, Map.class);

        if (fbUser != null && fbUser.get("email") != null) {
            String email = (String) fbUser.get("email");

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = User.builder()
                        .email(email)
                        .firstName((String) fbUser.get("first_name"))
                        .lastName((String) fbUser.get("last_name"))
                        .role(Role.FARMER)
                        .password(UUID.randomUUID().toString())
                        .score(50.0f)
                        .faceEnabled(false)
                        .status(tn.esprit.agri.entities.enums.Status.ACTIVE)
                        .build();
                return userRepository.save(newUser);
            });

            String token = jwtService.generateToken(user.getEmail(), "ROLE_" + user.getRole().name(), user.getFirstName(), user.getLastName(), user.getId());
            return ResponseEntity.ok(new LoginResponse(token));
        } else {
            throw new RuntimeException("Validation Facebook échouée ou email non fourni.");
        }
    }
}
