package tn.esprit.agri.services;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.agri.DTO.AiAnalysisResponse;
import tn.esprit.agri.DTO.AiQcmResponse;
import java.util.List;

public interface AiVerificationService {
    AiAnalysisResponse verifyClaim(String reason, String cropType, String location, MultipartFile image);
    List<AiQcmResponse> generateQcm(String cultureType);

    // New AI Question Bank methods
    List<tn.esprit.agri.entities.QuestionBank> getRandomQuestionsFromBank(String cultureType, int count);
    void feedQuestionBankAsync(String cultureType);
}
