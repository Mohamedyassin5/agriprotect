package tn.esprit.agri.services;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.agri.dto.AiAnalysisResponse;
import tn.esprit.agri.dto.AiQcmResponse;
import java.util.List;

public interface AiVerificationService {
    AiAnalysisResponse verifyClaim(String reason, String cropType, String location, MultipartFile image);
    List<AiQcmResponse> generateQcm(String cultureType);
}
