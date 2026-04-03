package tn.esprit.agri.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.agri.entities.Crop;
import tn.esprit.agri.entities.Sinistre;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.StatutSinistre;
import tn.esprit.agri.entities.enums.TypeSinistre;
import tn.esprit.agri.repositories.CropRepository;
import tn.esprit.agri.repositories.SinistreRepository;
import tn.esprit.agri.repositories.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class SinistreService {

    private final SinistreRepository sinistreRepository;
    private final CropRepository cropRepository;
    private final UserRepository userRepository;
    private final VisionAiService visionAiService;
    private final ObjectMapper objectMapper;

    private static final String UPLOAD_DIR = "uploads/sinistres/";

    @Transactional
    public Sinistre declareSinistre(String userId, String cropId, LocalDateTime dateCatastrophe, MultipartFile image, String description) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new RuntimeException("Crop not found"));

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = saveImage(image);
        }

        Sinistre sinistre = Sinistre.builder()
                .user(user)
                .crop(crop)
                .dateCatastrophe(dateCatastrophe)
                .imageUrl(imageUrl)
                .description(description)
                .statut(StatutSinistre.EN_ATTENTE)
                .isResolved(false)
                .build();

        // Analyse par l'IA
        if (image != null && !image.isEmpty()) {
            try {
                String aiResult = visionAiService.analyzeSinistreImage(image.getBytes(), image.getContentType());

                JsonNode root = objectMapper.readTree(aiResult);
                
                String typeStr = root.path("type").asText("AUTRE");
                float quota = (float) root.path("quota").asDouble(0.0);
                String aiDescription = root.path("description").asText("");

                sinistre.setTypeSinistre(TypeSinistre.valueOf(typeStr));
                sinistre.setQuotaRemboursement(quota);
                if (aiDescription != null && !aiDescription.isBlank()) {
                    sinistre.setDescription((sinistre.getDescription() != null ? sinistre.getDescription() + "\n\n" : "") + "Analyse IA : " + aiDescription);
                }
                
                if (quota > 0) {
                    sinistre.setStatut(StatutSinistre.VALIDE); // Auto-validation si quota > 0? Ou garder EN_ATTENTE?
                    // Mettons VALIDE si l'IA confirme un sinistre
                }
            } catch (Exception e) {
                log.error("AI Analysis failed for sinistre: ", e);
                sinistre.setTypeSinistre(TypeSinistre.AUTRE);
                sinistre.setQuotaRemboursement(0.0f);
            }
        } else {
            sinistre.setTypeSinistre(TypeSinistre.AUTRE);
            sinistre.setQuotaRemboursement(0.0f);
        }

        return sinistreRepository.save(sinistre);
    }

    @Transactional(readOnly = true)
    public List<Sinistre> getMySinistres(String userId) {
        if (userId == null) return sinistreRepository.findAll();
        return sinistreRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Sinistre getById(String id) {
        return sinistreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sinistre not found"));
    }

    @Transactional
    public void resolveSinistre(String sinistreId) {
        sinistreRepository.findById(sinistreId).ifPresent(s -> {
            s.setIsResolved(true);
            s.setResolvedAt(LocalDateTime.now());
            s.setStatut(StatutSinistre.RESOLU);
            sinistreRepository.save(s);
        });
    }

    private String saveImage(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        return UPLOAD_DIR + fileName;
    }
}
