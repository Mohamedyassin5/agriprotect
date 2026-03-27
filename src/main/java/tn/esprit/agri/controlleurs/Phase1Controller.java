package tn.esprit.agri.controlleurs;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.agri.dto.InsuranceResponse;
import tn.esprit.agri.dto.PaymentResponse;
import tn.esprit.agri.dto.PremiumEstimationResponse;
import tn.esprit.agri.dto.SignRequestDTO;
import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.*;
import tn.esprit.agri.repositories.InsuranceRepository;
import tn.esprit.agri.services.EmailService;
import tn.esprit.agri.services.IInsuranceService;
import tn.esprit.agri.services.IPremiumEstimationService;
import tn.esprit.agri.services.PaymentService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping("/agri/phase1")
@RequiredArgsConstructor
public class Phase1Controller {

    private final IPremiumEstimationService service;
    private final IInsuranceService insuranceService;
    private final InsuranceRepository insuranceRepository;
    private final EmailService emailService;
    private final PaymentService paymentService;

    @GetMapping("/estimate")
    public ResponseEntity<PremiumEstimationResponse> estimate(
            @Parameter(
                    name = "coverType",
                    description = "Type de couverture souhaité",
                    schema = @Schema(allowableValues = {"BASIC", "STANDARD", "PREMIUM"}, defaultValue = "STANDARD")
            )
            @RequestParam(defaultValue = "STANDARD") CoverageType coverType,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        User currentUser = (User) auth.getPrincipal();
        String userId = currentUser.getId();

        PremiumEstimationResponse response = service.calculateEstimation(userId, coverType);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<InsuranceResponse> subscribe(
            @RequestParam CoverageType coverType,
            @RequestParam PaymentMode paymentMode,   // ← NOUVEAU
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) auth.getPrincipal();
        String userId = user.getId();

        InsuranceResponse response = insuranceService.subscribe(userId, coverType, paymentMode);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-insurances")
    public ResponseEntity<List<InsuranceResponse>> getMyInsurances(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) auth.getPrincipal();
        List<Insurance> insurances = insuranceRepository.findByUserAndStatus(user, InsuranceStatus.ACTIVE);

        List<InsuranceResponse> responses = insurances.stream().map(ins -> {
            InsuranceResponse resp = new InsuranceResponse();
            resp.setId(ins.getId());
            resp.setPolicyNumber(ins.getPolicyNumber());
            resp.setCoverageType(ins.getCoverageType());
            resp.setInsuredAmount(ins.getInsuredAmount());
            resp.setPremiumAmount(ins.getPremiumAmount());
            resp.setStartDate(ins.getStartDate());
            resp.setEndDate(ins.getEndDate());
            resp.setStatus(ins.getStatus());
            resp.setMessage("Police active");
            return resp;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}/certificate.pdf")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    @Operation(summary = "Télécharger le certificat d'assurance (PDF) en français, anglais ou arabe")
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Langue du certificat (FR, EN, AR)")
            @RequestParam(defaultValue = "FR") Language lang) {

        Insurance insurance = insuranceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Police non trouvée"));

        if (currentUser.getRole() == Role.FARMER && !insurance.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette police ne vous appartient pas");
        }

        byte[] pdfBytes = insuranceService.generateInsuranceCertificatePdf(insurance, lang);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String fileName = "certificat_" + insurance.getPolicyNumber() + "_" + lang.name() + ".pdf";
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/{id}/sign/token")
    public ResponseEntity<InsuranceResponse> signViaToken(
            @PathVariable String id,
            @RequestParam String token,
            @RequestParam("signatureImage") MultipartFile signatureImage,
            @RequestParam("signatureName") String signatureName) {

        Insurance insurance = insuranceRepository.findById(id).orElseThrow();

        if (!token.equals(insurance.getSignToken()) || insurance.getSignTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lien invalide ou expiré");
        }

        if (insurance.getStatus() != InsuranceStatus.PENDING_SIGNATURE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        try {
            insurance.setSignatureImage(signatureImage.getBytes());
            insurance.setSignedByName(signatureName);
            insurance.setSignedAt(LocalDateTime.now());
            insurance.setStatus(InsuranceStatus.ACTIVE);

            insurance.setSignToken(null);
            insurance.setSignTokenExpiry(null);

            insurance = insuranceRepository.save(insurance);

            emailService.sendInsuranceConfirmationWithPdf(insurance);

            return ResponseEntity.ok(mapToResponse(insurance));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private InsuranceResponse mapToResponse(Insurance insurance) {
        InsuranceResponse response = new InsuranceResponse();
        response.setId(insurance.getId());
        response.setPolicyNumber(insurance.getPolicyNumber());
        response.setCoverageType(insurance.getCoverageType());
        response.setInsuredAmount(insurance.getInsuredAmount());
        response.setPremiumAmount(insurance.getPremiumAmount());
        response.setStartDate(insurance.getStartDate());
        response.setEndDate(insurance.getEndDate());
        response.setStatus(insurance.getStatus());
        response.setMessage("Police signée et active !");
        return response;
    }

    @PostMapping("/pay/{insuranceId}")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @PathVariable String insuranceId,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) auth.getPrincipal();
        PaymentResponse response = paymentService.initiateStripePayment(insuranceId, user.getId());

        return ResponseEntity.ok(response);
    }
    @GetMapping("/payment/success")
    public ResponseEntity<Map<String, Object>> paymentSuccess(
            @RequestParam String payment_intent,
            @RequestParam(required = false) String payment_intent_client_secret) {

        log.info("Page de succès atteinte - PaymentIntent: {}", payment_intent);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Paiement effectué avec succès !");
        response.put("paymentIntentId", payment_intent);

        // Optionnel : récupérer le statut réel
        try {
            String status = paymentService.getPaymentStatus(payment_intent);
            response.put("paymentStatus", status);
        } catch (Exception e) {
            response.put("paymentStatus", "unknown");
        }

        return ResponseEntity.ok(response);
    }
}