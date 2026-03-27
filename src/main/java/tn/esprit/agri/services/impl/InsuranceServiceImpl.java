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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.esprit.agri.dto.InsuranceResponse;
import tn.esprit.agri.dto.PremiumEstimationResponse;
import tn.esprit.agri.dto.SignRequestDTO;
import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.CoverageType;
import tn.esprit.agri.entities.enums.InsuranceStatus;
import tn.esprit.agri.entities.enums.Language;
import tn.esprit.agri.entities.enums.PaymentMode;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InsuranceServiceImpl implements IInsuranceService {

    private final IPremiumEstimationService estimationService;
    private final InsuranceRepository insuranceRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
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

        Insurance insurance = Insurance.builder()
                .user(user)
                .policyNumber(generatePolicyNumber())
                .coverageType(coverType)
                .paymentMode(paymentMode)
                .totalPremium(totalPremium)
                .amountPerPayment(amountPerPayment)
                .numberOfPayments(numberOfPayments)
                .remainingPayments(numberOfPayments)           // ← CORRECTION PRINCIPALE
                .insuredAmount(getInsuredAmount(estimation, coverType))
                .premiumAmount(annualPremium)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .status(InsuranceStatus.PENDING_SIGNATURE)
                .nextPaymentDue(LocalDate.now().plusMonths(1))   // première échéance
                .build();

        insurance = insuranceRepository.save(insurance);

        // Token de signature
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
}