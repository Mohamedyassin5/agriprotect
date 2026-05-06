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
import tn.esprit.agri.repositories.RisqueRepository;
import tn.esprit.agri.entities.Risque;

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
    private final RisqueRepository risqueRepository;
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
                float aiQuota = (float) root.path("quota").asDouble(0.0);
                String aiDescription = root.path("description").asText("");

                // ==========================================
                // LOUAY'S QUOTA CALCULATION LOGIC
                // ==========================================
                List<Risque> cropRisks = risqueRepository.findByCropId(cropId);
                float riskFactor = 1.0f;

                if (cropRisks != null && !cropRisks.isEmpty()) {
                    float totalRiskValue = 0f;
                    for (Risque r : cropRisks) {
                        if (r.getSeverity() == tn.esprit.agri.entities.enums.Severity.HIGH) {
                            totalRiskValue += 1.5f;
                        } else if (r.getSeverity() == tn.esprit.agri.entities.enums.Severity.MEDIUM) {
                            totalRiskValue += 1.2f;
                        } else if (r.getSeverity() == tn.esprit.agri.entities.enums.Severity.LOW) {
                            totalRiskValue += 1.0f;
                        }
                    }
                    riskFactor = totalRiskValue / cropRisks.size();
                }

                float surfaceFactor = 1.0f;
                if (crop.getSurface() != null && crop.getSurface() > 0) {
                    // Small bonus based on surface size (e.g. +1% per hectare)
                    surfaceFactor = 1.0f + (crop.getSurface().floatValue() * 0.01f);
                }

                // Final Quota combined
                float finalQuota = aiQuota * riskFactor * surfaceFactor;
                // Cap the quota at 1.0 (100%)
                finalQuota = Math.min(finalQuota, 1.0f);
                // ==========================================

                sinistre.setTypeSinistre(TypeSinistre.valueOf(typeStr));
                sinistre.setQuotaRemboursement(finalQuota);

                if (aiDescription != null && !aiDescription.isBlank()) {
                    sinistre.setDescription((sinistre.getDescription() != null ? sinistre.getDescription() + "\n\n" : "") +
                            "Analyse IA : " + aiDescription +
                            String.format("\n[Détails Calcul : Quota IA=%.2f, Facteur Risque=%.2f, Facteur Surface=%.2f, Quota Final=%.2f]",
                                    aiQuota, riskFactor, surfaceFactor, finalQuota));
                }

                if (finalQuota > 0) {
                    sinistre.setStatut(StatutSinistre.VALIDE); // Auto-validation si quota > 0
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

            try {
                tn.esprit.agri.services.EmailService emailService = org.springframework.web.context.support.WebApplicationContextUtils
                        .getRequiredWebApplicationContext(
                                ((org.springframework.web.context.request.ServletRequestAttributes)
                                        org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getRequest().getServletContext()
                        ).getBean(tn.esprit.agri.services.EmailService.class);
                emailService.sendSinistreResolvedEmail(s);
            } catch (Exception e) {
                log.error("Failed to send sinistre email", e);
            }
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
