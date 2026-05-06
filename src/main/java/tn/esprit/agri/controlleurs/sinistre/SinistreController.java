package tn.esprit.agri.controlleurs.sinistre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.agri.controlleurs.sinistre.dto.SinistreResponse;
import tn.esprit.agri.entities.Sinistre;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.SinistreService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/agri/sinistres")
@RequiredArgsConstructor
public class SinistreController {

    private final SinistreService sinistreService;
    private final UserRepository userRepository;
    private final tn.esprit.agri.services.EmailService emailService;

    @PostMapping(value = "/declare", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SinistreResponse> declareSinistre(
            Authentication authentication,
            @RequestParam("cropId") String cropId,
            @RequestParam("dateCatastrophe") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateCatastrophe,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "description", required = false) String description
    ) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Sinistre sinistre = sinistreService.declareSinistre(user.getId(), cropId, dateCatastrophe, image, description);
            return ResponseEntity.ok(toResponse(sinistre));
        } catch (Exception e) {
            log.error("Error declaring sinistre: ", e);
            throw new RuntimeException("Erreur lors de la déclaration du sinistre : " + e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<SinistreResponse>> getMySinistres(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(
                sinistreService.getMySinistres(user.getId())
                        .stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList())
        );
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<SinistreResponse> resolveSinistre(@PathVariable String id) {
        try {
            sinistreService.resolveSinistre(id);
            Sinistre resolved = sinistreService.getById(id);
            return ResponseEntity.ok(toResponse(resolved));
        } catch (Exception e) {
            log.error("Error resolving sinistre: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/unresolved/all")
    public ResponseEntity<List<SinistreResponse>> getUnresolvedSinistres() {
        return ResponseEntity.ok(
                sinistreService.getMySinistres(null)
                        .stream()
                        .filter(s -> !Boolean.TRUE.equals(s.getIsResolved()))
                        .map(this::toResponse)
                        .collect(Collectors.toList())
        );
    }



    private SinistreResponse toResponse(Sinistre s) {
        SinistreResponse r = new SinistreResponse();
        r.setId(s.getId());
        r.setCropId(s.getCrop().getId());
        r.setCropType(s.getCrop().getCropType());
        r.setUserId(s.getUser().getId());
        r.setUserEmail(s.getUser().getEmail());
        r.setDateCatastrophe(s.getDateCatastrophe());
        r.setImageUrl(s.getImageUrl());
        r.setTypeSinistre(s.getTypeSinistre());
        r.setQuotaRemboursement(s.getQuotaRemboursement());
        r.setDescription(s.getDescription());
        r.setStatut(s.getStatut());
        r.setCreatedAt(s.getCreatedAt());
        r.setResolvedAt(s.getResolvedAt());
        r.setIsResolved(s.getIsResolved());
        return r;
    }
}
