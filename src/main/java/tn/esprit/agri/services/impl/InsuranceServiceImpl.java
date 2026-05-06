package tn.esprit.agri.services.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import tn.esprit.agri.entities.enums.PaymentStatus;
import tn.esprit.agri.repositories.PaymentRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.agri.DTO.InsuranceResponse;
import tn.esprit.agri.DTO.PremiumEstimationResponse;
import tn.esprit.agri.DTO.SignRequestDTO;
import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.*;
import tn.esprit.agri.repositories.InsuranceRepository;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.EmailService;
import tn.esprit.agri.services.IInsuranceService;
import tn.esprit.agri.services.IPremiumEstimationService;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class InsuranceServiceImpl implements IInsuranceService {

    private final IPremiumEstimationService estimationService;
    private final InsuranceRepository insuranceRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    // 1. Ajouter l'injection en haut de la classe
    private final PaymentRepository paymentRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public InsuranceResponse subscribe(String userId, CoverageType coverType, PaymentMode paymentMode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        if (user.getScore() > 85) {
            throw new IllegalStateException("Score de risque trop élevé");
        }

        PremiumEstimationResponse estimation = estimationService.calculateEstimation(userId, coverType);

        // Calcul des valeurs selon le mode de paiement
        BigDecimal annualPremium = getPremiumAmount(estimation, coverType);

        BigDecimal totalPremium;
        BigDecimal amountPerPayment;
        int numberOfPayments;

        switch (paymentMode) {
            case MONTHLY -> {
                numberOfPayments = 12;
                amountPerPayment = annualPremium.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                totalPremium = annualPremium;
            }
            case QUARTERLY -> {
                numberOfPayments = 4;
                amountPerPayment = annualPremium.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
                totalPremium = annualPremium;
            }
            case SEMI_ANNUAL -> {
                numberOfPayments = 2;
                amountPerPayment = annualPremium.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                totalPremium = annualPremium;
            }
            case ANNUAL -> {
                numberOfPayments = 1;
                amountPerPayment = annualPremium;
                totalPremium = annualPremium;
            }
            default -> throw new IllegalArgumentException("Mode de paiement invalide");
        }

        // === CRÉATION DE L'ASSURANCE AVEC TOUS LES CHAMPS NÉCESSAIRES ===
        Insurance insurance = Insurance.builder()
                .user(user)
                .policyNumber(generatePolicyNumber())
                .coverageType(coverType)
                .paymentMode(paymentMode)
                .totalPremium(totalPremium)
                .amountPerPayment(amountPerPayment)
                .numberOfPayments(numberOfPayments)
                .remainingPayments(numberOfPayments)
                .insuredAmount(getInsuredAmount(estimation, coverType))
                .premiumAmount(annualPremium)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .status(InsuranceStatus.PENDING_SIGNATURE)
                .nextPaymentDue(LocalDate.now().plusMonths(1))

                // Champs critiques avec valeurs explicites
                .penaltyAmount(BigDecimal.ZERO)
                .isOverdue(false)

                .build();

        insurance = insuranceRepository.save(insurance);

        // Génération du token de signature
        insurance.setSignToken(UUID.randomUUID().toString());
        insurance.setSignTokenExpiry(LocalDateTime.now().plusDays(7));
        insurance = insuranceRepository.save(insurance);

        emailService.sendContractToSignEmail(insurance);

        InsuranceResponse response = mapToResponse(insurance);
        response.setMessage("Police créée avec mode de paiement " + paymentMode +
                " – en attente de votre signature électronique. Vérifiez votre email.");

        return response;
    }
    @Override
    public Insurance signInsurance(String insuranceId, String userId, SignRequestDTO dto) {
        Insurance insurance = insuranceRepository.findByIdAndUserId(insuranceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Police non trouvée ou non autorisée"));

        if (insurance.getStatus() != InsuranceStatus.PENDING_SIGNATURE) {
            throw new IllegalStateException("La police n'est pas en attente de signature");
        }

        insurance.setSignedAt(LocalDateTime.now());
        insurance.setSignedByName(dto.getSignatureName());
        insurance.setStatus(InsuranceStatus.ACTIVE);

        insurance = insuranceRepository.save(insurance);

        // Envoi de l'email de confirmation + PDF signé + lien vers le paiement
        emailService.sendInsuranceConfirmationWithPdf(insurance);   // ← tu peux garder ou renommer

        return insurance;
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
        response.setMessage("Police d'assurance créée avec succès ! Numéro : " + insurance.getPolicyNumber());
        return response;
    }
    private String generatePolicyNumber() {
        // Ajoute un timestamp pour éviter les doublons
        return String.format("AGRI-%d-%05d-%s",
                LocalDate.now().getYear(),
                insuranceRepository.count() + 1,
                UUID.randomUUID().toString().substring(0, 8)); // 8 caractères uniques
    }

    private BigDecimal getInsuredAmount(PremiumEstimationResponse estimation, CoverageType type) {
        String key = type.name();
        var detail = estimation.getDetailsByFormula().get(key);
        if (detail == null) {
            throw new IllegalStateException("Formule non trouvée dans l'estimation");
        }
        return detail.getInsuredAmount();
    }

    private BigDecimal getPremiumAmount(PremiumEstimationResponse estimation, CoverageType type) {
        String key = type.name();
        var detail = estimation.getDetailsByFormula().get(key);
        if (detail == null) {
            throw new IllegalStateException("Formule non trouvée dans l'estimation");
        }
        return detail.getPremiumAmount();
    }

    @Override
    public byte[] generateInsuranceCertificatePdf(Insurance insurance, Language lang) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            // Activation RTL pour l’arabe
            boolean isArabic = lang.isRtl();
            if (isArabic) {
                writer.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
                writer.setPageEmpty(false);
            }

            document.open();

            // -----------------------------------------------------------------
            // Polices (assure-toi que les fichiers sont dans src/main/resources/fonts/)
            // -----------------------------------------------------------------
            BaseFont bfLatinRegular = BaseFont.createFont("/fonts/NotoSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfLatinBold   = BaseFont.createFont("/fonts/NotoSans-Bold.ttf",   BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfArabicRegular = BaseFont.createFont("/fonts/NotoNaskhArabic-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfArabicBold   = BaseFont.createFont("/fonts/NotoNaskhArabic-Bold.ttf",   BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

            BaseFont titleFontBase  = isArabic ? bfArabicBold   : bfLatinBold;
            BaseFont subFontBase    = isArabic ? bfArabicRegular : bfLatinRegular;
            BaseFont labelFontBase  = isArabic ? bfArabicBold   : bfLatinBold;
            BaseFont valueFontBase  = isArabic ? bfArabicRegular : bfLatinRegular;
            BaseFont signFontBase   = isArabic ? bfArabicRegular : BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            BaseFont qrTextFontBase = isArabic ? bfArabicRegular : bfLatinRegular;
            BaseFont footerFontBase = isArabic ? bfArabicRegular : bfLatinRegular;

            Font titleFont  = new Font(titleFontBase, 18, Font.BOLD);
            Font subFont    = new Font(subFontBase,   12, Font.NORMAL);
            Font labelFont  = new Font(labelFontBase, 11, Font.BOLD);
            Font valueFont  = new Font(valueFontBase, 11, Font.NORMAL);
            Font signFont   = new Font(signFontBase,  14, Font.ITALIC);
            Font qrTextFont = new Font(qrTextFontBase, 9, Font.NORMAL);
            Font footerFont = new Font(footerFontBase, 8, Font.ITALIC);

            // ESPACEMENT
            document.add(new Paragraph("\n\n"));

            // TITRE
            Paragraph title = new Paragraph(getLocalizedTitle(lang), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // SOUS-TITRE
            Paragraph sub = new Paragraph(getLocalizedSubTitle(lang), subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            document.add(sub);
            document.add(new Paragraph("\n"));

            // LIGNE
            document.add(new Paragraph("───────────────────────────────────────────────"));

            document.add(new Paragraph("\n"));

            // TABLEAU
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(80);
            table.setSpacingBefore(10);
            table.setSpacingAfter(20);

            if (isArabic) {
                table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            }

            String[][] rows = getLocalizedRows(insurance, lang);
            for (String[] row : rows) {
                PdfPCell label = new PdfPCell(new Paragraph(row[0], labelFont));
                label.setPadding(8);
                label.setBorderWidth(0.5f);
                label.setHorizontalAlignment(isArabic ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);

                PdfPCell value = new PdfPCell(new Paragraph(row[1], valueFont));
                value.setPadding(8);
                value.setBorderWidth(0.5f);
                value.setHorizontalAlignment(Element.ALIGN_LEFT);

                table.addCell(label);
                table.addCell(value);
            }

            document.add(table);

            document.add(new Paragraph("\n\n\n"));

            // SIGNATURE + QR
            PdfPTable signQr = new PdfPTable(2);
            signQr.setWidthPercentage(80);

            // Signature
            PdfPCell signCell = new PdfPCell();
            signCell.setBorder(Rectangle.NO_BORDER);
            signCell.setHorizontalAlignment(isArabic ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);

            String signName = getLocalizedSignatureName(lang);
            signCell.addElement(new Paragraph(signName, signFont));
            signCell.addElement(new Paragraph("______________________________", signFont));

            signQr.addCell(signCell);

            // QR Code
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(isArabic ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);

            String verifyUrl = "http://localhost:8081/api/insurances/verify?policy=" + insurance.getPolicyNumber();
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix matrix = qrWriter.encode(verifyUrl, BarcodeFormat.QR_CODE, 120, 120);
            BufferedImage qrImg = MatrixToImageWriter.toBufferedImage(matrix);

            Image qrImage = Image.getInstance(qrImg, null);
            qrImage.scaleAbsolute(80, 80);
            qrCell.addElement(qrImage);

            String qrLabel = getLocalizedQrLabel(lang);
            qrCell.addElement(new Paragraph(qrLabel, qrTextFont));

            signQr.addCell(qrCell);

            document.add(signQr);
// Après le tableau et avant le footer
            if (insurance.getSignatureImage() != null) {
                Image signImg = Image.getInstance(insurance.getSignatureImage());
                signImg.scaleToFit(200, 80);
                signImg.setAlignment(Element.ALIGN_CENTER);
                document.add(new Paragraph("Signature :"));
                document.add(signImg);
                document.add(new Paragraph("Signé par : " + insurance.getSignedByName()));
            }
            // PIED DE PAGE
            String footerText = getLocalizedFooter(lang);
            Paragraph footer = new Paragraph(footerText, footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(40);
            document.add(footer);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF : " + e.getMessage(), e);
        }
    }

    // Helpers de traduction (comme dans ton ancien code)
    private String getLocalizedTitle(Language lang) {
        return switch (lang) {
            case AR -> "شهادة التأمين";
            case EN -> "INSURANCE CERTIFICATE";
            default -> "CERTIFICAT D'ASSURANCE";
        };
    }

    private String getLocalizedSubTitle(Language lang) {
        return switch (lang) {
            case AR -> "أغري بروتكت";
            default -> "AGRI PROTECT";
        };
    }

    private String getLocalizedSignatureName(Language lang) {
        return switch (lang) {
            case AR -> "ج. بن جنّات\nالمدير التقني";
            case EN -> "G. Ben Jannet\nTechnical Director";
            default -> "G. Ben Jannet\nDirecteur Technique";
        };
    }

    private String getLocalizedQrLabel(Language lang) {
        return switch (lang) {
            case AR -> "امسح للتحقق";
            case EN -> "Scan to verify";
            default -> "Scanner pour vérification";
        };
    }

    private String getLocalizedFooter(Language lang) {
        return switch (lang) {
            case AR -> "وثيقة مولدة تلقائياً بواسطة أغري بروتكت";
            case EN -> "Document automatically generated by AgriProtect";
            default -> "Document généré automatiquement par AgriProtect";
        };
    }

    private String[][] getLocalizedRows(Insurance insurance, Language lang) {
        boolean isAr = lang == Language.AR;
        boolean isEn = lang == Language.EN;

        User user = insurance.getUser();

        return new String[][] {
                {isAr ? "رقم الوثيقة" : (isEn ? "Policy Number" : "Numéro de police"), insurance.getPolicyNumber() != null ? insurance.getPolicyNumber() : "—"},
                {isAr ? "المؤمن له" : (isEn ? "Insured" : "Assuré"), getFullName(user)},
                {isAr ? "البريد الإلكتروني" : (isEn ? "Email" : "Email"), user.getEmail() != null ? user.getEmail() : "—"},
                {isAr ? "نوع التغطية" : (isEn ? "Coverage Type" : "Type de couverture"), insurance.getCoverageType() != null ? insurance.getCoverageType().name() : "—"},
                {isAr ? "المبلغ المؤمن عليه" : (isEn ? "Insured Amount" : "Montant assuré"), String.format("%,.2f TND", insurance.getInsuredAmount())},
                {isAr ? "الاشتراك السنوي" : (isEn ? "Annual Premium" : "Prime annuelle"), String.format("%,.2f TND", insurance.getPremiumAmount())},
                {isAr ? "تاريخ البدء" : (isEn ? "Start Date" : "Date de début"), insurance.getStartDate() != null ? insurance.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—"},
                {isAr ? "تاريخ الانتهاء" : (isEn ? "End Date" : "Date de fin"), insurance.getEndDate() != null ? insurance.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—"},
                {isAr ? "الحالة" : (isEn ? "Status" : "Statut"), insurance.getStatus() != null ? insurance.getStatus().name() : "—"}
        };
    }

    private String getFullName(User user) {
        if (user == null) return "Non renseigné";
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last  = user.getLastName()  != null ? user.getLastName().trim()  : "";
        String full  = (first + " " + last).trim();
        return full.isEmpty() ? "Non renseigné" : full;
    }
    @Override
    public Insurance findByIdWithAuthorization(String insuranceId, User currentUser) {
        if (insuranceId == null || insuranceId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de police invalide");
        }

        Insurance insurance = insuranceRepository.findById(insuranceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Police non trouvée"));

        // Vérification des autorisations
        if (currentUser.getRole() == Role.FARMER) {
            if (!insurance.getUser().getId().equals(currentUser.getId())) {
                log.warn("Tentative d'accès non autorisé à la police {} par l'utilisateur {}",
                        insuranceId, currentUser.getId());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette police ne vous appartient pas");
            }
        }
        // Les admins ont accès à toutes les polices

        return insurance;
    }
    @Override
    public void cancelPendingSubscription(String insuranceId, String userId) {
        if (insuranceId == null || insuranceId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de police invalide");
        }

        Insurance insurance = insuranceRepository.findById(insuranceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Police non trouvée"));

        // Vérification que l'utilisateur est bien le propriétaire
        if (!insurance.getUser().getId().equals(userId)) {
            log.warn("Tentative d'annulation non autorisée de la police {} par l'utilisateur {}", insuranceId, userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette police ne vous appartient pas");
        }

        // On autorise l'annulation uniquement si la police est encore en attente
        if (insurance.getStatus() != InsuranceStatus.PENDING_SIGNATURE) {
            log.warn("Impossible d'annuler la police {} car son statut est {}", insuranceId, insurance.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seules les polices en attente de signature peuvent être annulées. Statut actuel : " + insurance.getStatus());
        }

        // Annulation de la police
        insurance.setStatus(InsuranceStatus.CANCELLED);
        insurance.setCancelledAt(LocalDateTime.now());           // Assure-toi que ce champ existe dans l'entité
        insurance.setCancellationReason("Annulé par l'utilisateur avant signature");

        insuranceRepository.save(insurance);

        log.info("Police {} annulée avec succès par l'utilisateur {}", insuranceId, userId);

        // Optionnel : envoi d'email
        // emailService.sendCancellationConfirmation(insurance);
    }
    @Override
    public Map<String, Object> getFarmerDashboard(String userId) {
        Map<String, Object> dashboard = new HashMap<>();

        try {
            long totalPolicies = insuranceRepository.countByUserId(userId);
            long activePolicies = insuranceRepository.countByUserIdAndStatus(userId, InsuranceStatus.ACTIVE);
            long pendingPolicies = insuranceRepository.countByUserIdAndStatus(userId, InsuranceStatus.PENDING_SIGNATURE);
            long overduePolicies = insuranceRepository.countByUserIdAndStatus(userId, InsuranceStatus.OVERDUE);
            long suspendedPolicies = insuranceRepository.countByUserIdAndStatus(userId, InsuranceStatus.SUSPENDED);
            long cancelledPolicies = insuranceRepository.countByUserIdAndStatus(userId, InsuranceStatus.CANCELLED);

            LocalDate nextPaymentDate = insuranceRepository.findNextPaymentDueByUserId(userId).orElse(null);
            BigDecimal totalPremiumDue = insuranceRepository.calculateTotalPremiumDueThisYear(userId);

            // ── NOUVEAU : total réellement payé ──────────────────────────────
            BigDecimal totalPremiumPaid = paymentRepository
                    .findByInsuranceUserIdOrderByPaymentDateDesc(userId)
                    .stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                    .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // ─────────────────────────────────────────────────────────────────

            dashboard.put("totalPolicies", totalPolicies);
            dashboard.put("activePolicies", activePolicies);
            dashboard.put("pendingPolicies", pendingPolicies);
            dashboard.put("overduePolicies", overduePolicies);
            dashboard.put("suspendedPolicies", suspendedPolicies);
            dashboard.put("cancelledPolicies", cancelledPolicies);
            dashboard.put("nextPaymentDue", nextPaymentDate);
            dashboard.put("totalPremiumDueThisYear", totalPremiumDue != null ? totalPremiumDue : BigDecimal.ZERO);
            dashboard.put("totalPremiumPaid", totalPremiumPaid); // ← NOUVEAU

            Map<String, Long> statusBreakdown = new HashMap<>();
            statusBreakdown.put("ACTIVE", activePolicies);
            statusBreakdown.put("PENDING_SIGNATURE", pendingPolicies);
            statusBreakdown.put("OVERDUE", overduePolicies);
            statusBreakdown.put("SUSPENDED", suspendedPolicies);
            statusBreakdown.put("CANCELLED", cancelledPolicies);
            dashboard.put("statusBreakdown", statusBreakdown);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du dashboard pour user {}", userId, e);
            dashboard.put("error", "Erreur partielle lors du chargement des données");
            dashboard.put("totalPremiumPaid", BigDecimal.ZERO); // ← fallback
        }

        return dashboard;
    }
    private void addSinistreClausesToDocument(Document document, Insurance insurance, Language lang,
                                              Font titleFont, Font labelFont, Font valueFont) throws Exception {

        boolean isAr = lang == Language.AR;
        boolean isEn = lang == Language.EN;

        // Titre section
        String sectionTitle = isAr ? "شروط وأحكام التعويض عن الأضرار"
                : isEn ? "CLAIMS & COMPENSATION CONDITIONS"
                : "CONDITIONS DE SINISTRE ET REMBOURSEMENT";

        Paragraph clauseTitle = new Paragraph(sectionTitle, titleFont);
        clauseTitle.setAlignment(isAr ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        clauseTitle.setSpacingBefore(20);
        clauseTitle.setSpacingAfter(10);
        document.add(clauseTitle);

        document.add(new Paragraph("───────────────────────────────────────────────────────"));
        document.add(new Paragraph("\n"));

        // Calcul des seuils selon la couverture
        BigDecimal insuredAmount = insurance.getInsuredAmount();
        BigDecimal coverageRate  = getCoverageRateFromType(insurance.getCoverageType());
        BigDecimal franchiseRate = getFranchiseRateFromType(insurance.getCoverageType());

        BigDecimal maxReimbursement   = insuredAmount.multiply(coverageRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal franchiseThreshold = insuredAmount.multiply(franchiseRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal partialThreshold   = insuredAmount.multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP);

        // === CLAUSE 1 : Franchise ===
        String clause1Title = isAr ? "الشرط الأول : الحد الأدنى للتعويض (الفرنشيز)"
                : isEn ? "CLAUSE 1 : Deductible (Franchise)"
                : "CLAUSE 1 : Franchise (Seuil minimum de sinistre)";

        String clause1Body = isAr
                ? String.format("لا يُمنح أي تعويض إذا كانت الأضرار تقل عن نسبة %.0f%% من المبلغ المؤمن عليه، أي ما يعادل %.2f دينار تونسي. يتحمل المؤمن له هذا الجزء كلياً.",
                franchiseRate.multiply(BigDecimal.valueOf(100)).doubleValue(), franchiseThreshold.doubleValue())
                : isEn
                ? String.format("No compensation shall be granted if the claim amount is below %.0f%% of the insured amount (%.2f TND). The policyholder bears this portion entirely.",
                franchiseRate.multiply(BigDecimal.valueOf(100)).doubleValue(), franchiseThreshold.doubleValue())
                : String.format("Aucune indemnisation ne sera accordée si le montant du sinistre est inférieur à %.0f%% du montant assuré, soit %.2f TND. Cette franchise reste entièrement à la charge de l'assuré.",
                franchiseRate.multiply(BigDecimal.valueOf(100)).doubleValue(), franchiseThreshold.doubleValue());

        addClause(document, "1", clause1Title, clause1Body, labelFont, valueFont, isAr);

        // === CLAUSE 2 : Taux de remboursement et plafond ===
        String clause2Title = isAr ? "الشرط الثاني : نسبة وسقف التعويض"
                : isEn ? "CLAUSE 2 : Reimbursement Rate & Cap"
                : "CLAUSE 2 : Taux et plafond de remboursement";

        String clause2Body = isAr
                ? String.format("في حال وقوع حادث زراعي، يُعوَّض المؤمن له بنسبة %.0f%% من الأضرار المثبتة، مع سقف أقصى يبلغ %.2f دينار تونسي. تُحدَّد نسبة التعويض الفعلية حسب تقرير الخبير المعيَّن من الشركة.",
                coverageRate.multiply(BigDecimal.valueOf(100)).doubleValue(), maxReimbursement.doubleValue())
                : isEn
                ? String.format("In case of an agricultural loss, the insured shall be compensated at a rate of %.0f%% of the documented damages, up to a maximum of %.2f TND. The actual reimbursement rate shall be determined by the company's appointed expert.",
                coverageRate.multiply(BigDecimal.valueOf(100)).doubleValue(), maxReimbursement.doubleValue())
                : String.format("En cas de sinistre agricole avéré, l'assuré sera indemnisé à hauteur de %.0f%% des dommages constatés, dans la limite d'un plafond de %.2f TND. Le taux réel sera déterminé par l'expert mandaté par la compagnie.",
                coverageRate.multiply(BigDecimal.valueOf(100)).doubleValue(), maxReimbursement.doubleValue());

        addClause(document, "2", clause2Title, clause2Body, labelFont, valueFont, isAr);

        // === CLAUSE 3 : Délai de déclaration et pièces justificatives ===
        String clause3Title = isAr ? "الشرط الثالث : آجال التصريح والوثائق المطلوبة"
                : isEn ? "CLAUSE 3 : Declaration Deadline & Required Documents"
                : "CLAUSE 3 : Délai de déclaration et pièces justificatives";

        String clause3Body = isAr
                ? String.format("يجب على المؤمن له التصريح بالأضرار في أجل لا يتجاوز 72 ساعة من تاريخ وقوع الحادث. يستلزم الملف: صورة عن الضرر، تقرير طبيعي أو تقني، وثيقة إثبات المساحة (%.2f دينار تونسي حد أدنى للأضرار المعترف بها). كل تصريح مؤخر قد يؤدي إلى تخفيض التعويض أو رفضه.",
                partialThreshold.doubleValue())
                : isEn
                ? String.format("The insured must declare any claim within 72 hours of the incident. Required documents: damage photos, natural or technical report, land ownership proof (minimum damage threshold: %.2f TND). Late declarations may result in reduced or denied compensation.",
                partialThreshold.doubleValue())
                : String.format("L'assuré doit déclarer tout sinistre dans un délai maximum de 72 heures après l'incident. Le dossier doit comprendre : photos des dommages, rapport d'expert ou météorologique, justificatif de surface (seuil minimum de dommages reconnus : %.2f TND). Tout retard de déclaration pourra entraîner une réduction ou un refus d'indemnisation.",
                partialThreshold.doubleValue());

        addClause(document, "3", clause3Title, clause3Body, labelFont, valueFont, isAr);

        document.add(new Paragraph("\n"));
    }

    // Helper pour ajouter une clause visuellement distincte
    private void addClause(Document document, String number, String title, String body,
                           Font labelFont, Font valueFont, boolean isAr) throws Exception {

        // Conteneur de la clause
        PdfPTable clauseTable = new PdfPTable(1);
        clauseTable.setWidthPercentage(90);
        clauseTable.setSpacingBefore(8);
        clauseTable.setSpacingAfter(8);

        if (isAr) clauseTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

        PdfPCell cell = new PdfPCell();
        cell.setPadding(12);
        cell.setBorderWidth(1f);
        cell.setBorderColor(new java.awt.Color(22, 163, 74)); // vert AgriProtect
        cell.setBackgroundColor(new java.awt.Color(240, 253, 244)); // fond vert très clair

        // Titre de la clause
        Paragraph clauseTitlePara = new Paragraph(title, labelFont);
        clauseTitlePara.setAlignment(isAr ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        clauseTitlePara.setSpacingAfter(6);
        cell.addElement(clauseTitlePara);

        // Corps de la clause
        Paragraph clauseBodyPara = new Paragraph(body, valueFont);
        clauseBodyPara.setAlignment(isAr ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        clauseBodyPara.setLeading(16);
        cell.addElement(clauseBodyPara);

        clauseTable.addCell(cell);
        document.add(clauseTable);
    }

    // Helpers pour récupérer les taux selon le type de couverture
    private BigDecimal getCoverageRateFromType(CoverageType type) {
        if (type == null) return BigDecimal.valueOf(0.75);
        return switch (type) {
            case BASIC    -> BigDecimal.valueOf(0.60);
            case PREMIUM  -> BigDecimal.valueOf(0.90);
            default       -> BigDecimal.valueOf(0.75); // STANDARD
        };
    }

    private BigDecimal getFranchiseRateFromType(CoverageType type) {
        if (type == null) return BigDecimal.valueOf(0.20);
        return switch (type) {
            case BASIC    -> BigDecimal.valueOf(0.30);
            case PREMIUM  -> BigDecimal.valueOf(0.10);
            default       -> BigDecimal.valueOf(0.20); // STANDARD
        };
    }
}