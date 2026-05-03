package tn.esprit.agri.controlleurs.assistant;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.agri.controlleurs.assistant.dto.ChatRequest;
import tn.esprit.agri.controlleurs.assistant.dto.ChatResponse;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.AssistantService;

@Slf4j
@RestController
@RequestMapping("/agri/assistant")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssistantController {

    private final AssistantService assistantService;
    private final UserRepository userRepository;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatResponse response = assistantService.chat(user, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AgriAssistant Running with Groq/Llama3");
    }
}
