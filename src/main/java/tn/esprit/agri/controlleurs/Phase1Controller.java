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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
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
import tn.esprit.agri.entities.Payment;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.*;
import tn.esprit.agri.repositories.InsuranceRepository;
import tn.esprit.agri.repositories.PaymentRepository;
import tn.esprit.agri.services.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final PaymentReminderService paymentReminderService;
    private final PaymentRepository paymentRepository;
    private final IAIRiskAssessmentService aiRiskService;

    @GetMapping("/estimate")
    @Operation(summary = "Estimer avec IA")
    public ResponseEntity<PremiumEstimationResponse> estimate(
            @RequestParam(defaultValue = "STANDARD") CoverageType coverType,
            Authentication authentication) {   // ← Garde Authentication

        // Vérification plus robuste
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null); // ou throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        User currentUser = (User) authentication.getPrincipal();

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            PremiumEstimationResponse response = service.calculateEstimation(currentUser.getId(), coverType);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de l'estimation du premium pour user {}", currentUser.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/estimate/ai-complete")
    @Operation(summary = "Estimer avec IA complète (l'IA calcule tout)")
    public ResponseEntity<PremiumEstimationResponse> estimateWithAIComplete(
            @RequestParam(defaultValue = "STANDARD") CoverageType coverType,
            Authentication authentication) {

        log.info("=== DÉBUT estimateWithAIComplete ===");

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Authentication null ou non authentifié");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = (User) authentication.getPrincipal();
        log.info("User ID: {}", currentUser.getId());

        try {
            IAIRiskAssessmentService.AICompleteEstimationResult aiResult =
                    aiRiskService.calculateCompleteEstimation(currentUser.getId(), coverType);

            if (aiResult == null || aiResult.detailsByFormula() == null) {
                log.error("Résultat IA null pour user {}", currentUser.getId());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Utiliser la formule suggérée par l'IA pour le totalPremium
            String finalFormula = aiResult.suggestedFormula();
            var suggestedDetail = aiResult.detailsByFormula().get(finalFormula);

            if (suggestedDetail == null) {
                log.warn("Formule suggérée {} non trouvée, fallback sur {}", finalFormula, coverType.name());
                finalFormula = coverType.name();
                suggestedDetail = aiResult.detailsByFormula().get(finalFormula);
            }

            log.info("Résultat IA: riskScore={}, suggestedFormula={}, totalPremium={}",
                    aiResult.riskScore(), finalFormula, suggestedDetail.getPremiumAmount());

            PremiumEstimationResponse response = PremiumEstimationResponse.builder()
                    .totalPremium(suggestedDetail.getPremiumAmount())
                    .detailsByFormula(aiResult.detailsByFormula())
                    .suggestedFormula(finalFormula)
                    .suggestedInsuredAmount(suggestedDetail.getInsuredAmount())
                    .minAllowedInsuredAmount(suggestedDetail.getInsuredAmount().multiply(BigDecimal.valueOf(0.8)))
                    .maxAllowedInsuredAmount(suggestedDetail.getInsuredAmount().multiply(BigDecimal.valueOf(1.2)))
                    .aiRiskScore(aiResult.riskScore())
                    .riskLevel(aiResult.riskLevel())
                    .riskFactors(aiResult.riskFactors())
                    .aiInsights(aiResult.insights())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de l'estimation IA complète", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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

    @GetMapping("/{id}")
    public ResponseEntity<InsuranceResponse> getInsuranceById(
            @PathVariable String id,
            Authentication auth) {

        User user = (User) auth.getPrincipal();
        Insurance insurance = insuranceService.findByIdWithAuthorization(id, user);

        return ResponseEntity.ok(mapToResponse(insurance));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<Void> cancelSubscription(
            @PathVariable String id,
            Authentication auth) {

        User user = (User) auth.getPrincipal();
        insuranceService.cancelPendingSubscription(id, user.getId());
        return ResponseEntity.noContent().build();
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

    // ====================== TEST 1 : Rappel 2 jours avant ======================
    @GetMapping("/reminder/2days/{insuranceId}")
    public String testReminder2Days(@PathVariable String insuranceId) {
        Insurance insurance = insuranceRepository.findById(insuranceId)
                .orElseThrow(() -> new RuntimeException("Insurance not found"));

        insurance.setNextPaymentDue(LocalDate.now().plusDays(2)); // échéance dans 2 jours
        insurance.setOverdue(false);
        insurance.setPenaltyAmount(BigDecimal.ZERO);
        insurance.setStatus(InsuranceStatus.ACTIVE);

        insuranceRepository.save(insurance);

        return "✅ Test Rappel 2 jours activé pour la police : " + insurance.getPolicyNumber() +
                "\nDate d'échéance : " + insurance.getNextPaymentDue();
    }

    // ====================== TEST 2 : Pénalité après 7 jours de retard ======================
    @GetMapping("/penalty/7days/{insuranceId}")
    public String testPenalty7Days(@PathVariable String insuranceId) {
        Insurance insurance = insuranceRepository.findById(insuranceId)
                .orElseThrow(() -> new RuntimeException("Insurance not found"));

        insurance.setNextPaymentDue(LocalDate.now().minusDays(7)); // 7 jours de retard
        insurance.setOverdue(false);
        insurance.setPenaltyAmount(BigDecimal.ZERO);
        insurance.setStatus(InsuranceStatus.ACTIVE);

        insuranceRepository.save(insurance);

        return "✅ Test Pénalité (7 jours retard) activé pour la police : " + insurance.getPolicyNumber() +
                "\nDate d'échéance : " + insurance.getNextPaymentDue();
    }

    // ====================== TEST 3 : Suspension après 15 jours ======================
    @GetMapping("/suspend/15days/{insuranceId}")
    public String testSuspend15Days(@PathVariable String insuranceId) {
        Insurance insurance = insuranceRepository.findById(insuranceId)
                .orElseThrow(() -> new RuntimeException("Insurance not found"));

        insurance.setNextPaymentDue(LocalDate.now().minusDays(15)); // 15 jours de retard
        insurance.setOverdue(true);
        insurance.setStatus(InsuranceStatus.ACTIVE);

        insuranceRepository.save(insurance);

        return "✅ Test Suspension (15 jours retard) activé pour la police : " + insurance.getPolicyNumber() +
                "\nDate d'échéance : " + insurance.getNextPaymentDue() +
                "\n\nExécutez /run-reminder pour forcer le traitement.";
    }

    // ====================== FORCER L'EXÉCUTION DU SCHEDULER ======================
    @GetMapping("/run-reminder")
    public String runReminderNow() {
        try {
            paymentReminderService.checkAndSendReminders();
            return "✅ Scheduler exécuté manuellement avec succès ! Vérifie tes emails et la base de données.";
        } catch (Exception e) {
            return "❌ Erreur lors de l'exécution du scheduler : " + e.getMessage();
        }
    }

    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<Payment>> getMyPayments(Authentication auth) {
        User user = (User) auth.getPrincipal();
        List<Payment> payments = paymentRepository.findByInsuranceUserIdOrderByPaymentDateDesc(user.getId());
        return ResponseEntity.ok(payments);
    }

    // Historique complet d'une police (admin ou farmer propriétaire)
    @GetMapping("/{insuranceId}/payments")
    public ResponseEntity<List<Payment>> getPaymentsByInsurance(
            @PathVariable String insuranceId,
            Authentication auth) {

        Insurance insurance = insuranceRepository.findById(insuranceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Vérification que c'est bien le propriétaire ou un admin
        User user = (User) auth.getPrincipal();
        if (user.getRole() == Role.FARMER && !insurance.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<Payment> payments = paymentRepository.findByInsuranceOrderByPaymentDateDesc(insurance);
        return ResponseEntity.ok(payments);
    }
    // ====================== ADMIN SECTION ======================

    @GetMapping("/admin/insurances")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InsuranceResponse>> getAllInsurances(
            @RequestParam(required = false) InsuranceStatus status) {

        List<Insurance> insurances = (status != null)
                ? insuranceRepository.findByStatus(status)
                : insuranceRepository.findAll();

        List<InsuranceResponse> responses = insurances.stream()
                .map(this::mapToResponse)           // Cette méthode existe déjà dans ce controller
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // Statistiques globales pour l'admin
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPolicies", insuranceRepository.count());
        stats.put("activePolicies", insuranceRepository.countByStatus(InsuranceStatus.ACTIVE));
        stats.put("overduePolicies", insuranceRepository.countByStatus(InsuranceStatus.OVERDUE));
        stats.put("suspendedPolicies", insuranceRepository.countByStatus(InsuranceStatus.SUSPENDED));
        stats.put("completedPolicies", insuranceRepository.countByStatus(InsuranceStatus.COMPLETED));

        // Chiffre d'affaires du mois en cours
        double revenueThisMonth = paymentRepository.findAll().stream()
                .filter(p -> p.getPaymentDate() != null &&
                        p.getPaymentDate().getMonth() == LocalDate.now().getMonth())
                .mapToDouble(p -> p.getAmount().doubleValue())
                .sum();

        stats.put("totalRevenueThisMonth", revenueThisMonth);

        return ResponseEntity.ok(stats);
    }

    // Polices en retard ou suspendues
    @GetMapping("/admin/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InsuranceResponse>> getOverdueInsurances() {
        List<Insurance> overdue = insuranceRepository.findByStatusIn(
                List.of(InsuranceStatus.OVERDUE, InsuranceStatus.SUSPENDED));

        List<InsuranceResponse> responses = overdue.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // ====================== RÉGULARISATION DE POLICE SUSPENDUE ======================
    @PostMapping("/{insuranceId}/regularize")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    @Operation(summary = "Régulariser une police suspendue ou en retard (payer les arriérés + réactiver)")
    public ResponseEntity<InsuranceResponse> regularizeSuspendedPolicy(
            @PathVariable String insuranceId,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = (User) auth.getPrincipal();

        try {
            // Appel au service de régularisation
            Insurance insurance = paymentService.regularizePayment(insuranceId, currentUser.getId());

            log.info("Police {} régularisée avec succès par l'utilisateur {}",
                    insurance.getPolicyNumber(), currentUser.getId());

            return ResponseEntity.ok(mapToResponse(insurance));

        } catch (IllegalArgumentException e) {
            log.warn("Erreur de régularisation : {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("État invalide pour régularisation : {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Erreur lors de la régularisation de la police {}", insuranceId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne lors de la régularisation");
        }
    }

    // ====================== RÉGULARISATION AVEC PAIEMENT ======================
    @PostMapping("/{insuranceId}/regularize-payment")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    @Operation(summary = "Initier le paiement pour régulariser une police suspendue")
    public ResponseEntity<PaymentResponse> initiateRegularizationPayment(
            @PathVariable String insuranceId,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) auth.getPrincipal();

        PaymentResponse response = paymentService.initiateRegularizationPayment(insuranceId, user.getId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('FARMER')")
    @Operation(summary = "Tableau de bord farmer : résumé des polices et paiements")
    public ResponseEntity<Map<String, Object>> getFarmerDashboard(Authentication auth) {
        User user = (User) auth.getPrincipal();

        Map<String, Object> dashboard = insuranceService.getFarmerDashboard(user.getId());

        return ResponseEntity.ok(dashboard);
    }

    // 3. Télécharger la facture / quittance de paiement
    @GetMapping("/{insuranceId}/invoice.pdf")
    public ResponseEntity<byte[]> downloadInvoice(
            @PathVariable String insuranceId,
            Authentication auth) {

        User user = (User) auth.getPrincipal();
        byte[] pdf = paymentService.generateInvoicePdf(insuranceId, user.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("facture_" + insuranceId + ".pdf").build());

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}