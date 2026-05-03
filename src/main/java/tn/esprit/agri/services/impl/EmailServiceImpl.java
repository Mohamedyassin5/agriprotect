package tn.esprit.agri.services.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.Language;
import tn.esprit.agri.services.EmailService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void sendInsuranceConfirmationWithPdf(Insurance insurance) {
        User user = insurance.getUser();
        String toEmail = user.getEmail();

        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Pas d'email pour l'utilisateur {}", user.getId());
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Votre police AgriProtect est signée et active !");

            String paymentLink = "http://localhost:8081/payment.html?insuranceId=" + insurance.getId();

            String paymentFrequency = switch (insurance.getPaymentMode()) {
                case MONTHLY -> "mensuel";
                case QUARTERLY -> "trimestriel";
                case SEMI_ANNUAL -> "semestriel";
                case ANNUAL -> "annuel";
            };

            String body = String.format(
                    "Cher %s %s,\n\n" +
                            "Félicitations ! Votre contrat a été signé avec succès.\n" +
                            "Votre police d'assurance est maintenant **ACTIVE**.\n\n" +

                            "Détails de votre police :\n" +
                            "- Numéro de police : %s\n" +
                            "- Type de couverture : %s\n" +
                            "- Montant assuré : %.2f TND\n" +
                            "- Prime annuelle : %.2f TND\n" +
                            "- Mode de paiement : %s\n" +
                            "- Montant par échéance : %.2f TND (%s)\n" +
                            "- Date de début : %s\n" +
                            "- Date de fin : %s\n\n" +

                            "Pour maintenir votre couverture active, effectuez votre premier paiement.\n\n" +
                            "Montant à payer : **%.2f TND**\n" +
                            "Fréquence : %s\n" +
                            "Prochain paiement dû le : %s\n\n" +

                            "Cliquez ici pour payer :\n%s\n\n" +

                            "Le certificat d'assurance signé est joint en PDF.\n\n" +
                            "Merci pour votre confiance,\nL'équipe AgriProtect",

                    user.getFirstName() != null ? user.getFirstName() : "",
                    user.getLastName() != null ? user.getLastName() : "",
                    insurance.getPolicyNumber(),
                    insurance.getCoverageType(),
                    insurance.getInsuredAmount() != null ? insurance.getInsuredAmount() : BigDecimal.ZERO,
                    insurance.getPremiumAmount() != null ? insurance.getPremiumAmount() : BigDecimal.ZERO,
                    insurance.getPaymentMode(),
                    insurance.getAmountPerPayment() != null ? insurance.getAmountPerPayment() : BigDecimal.ZERO,
                    paymentFrequency,
                    insurance.getStartDate() != null ? insurance.getStartDate().format(DATE_FORMAT) : "—",
                    insurance.getEndDate() != null ? insurance.getEndDate().format(DATE_FORMAT) : "—",
                    insurance.getAmountPerPayment() != null ? insurance.getAmountPerPayment() : BigDecimal.ZERO,
                    paymentFrequency,
                    insurance.getNextPaymentDue() != null ? insurance.getNextPaymentDue().format(DATE_FORMAT) : "—",
                    paymentLink
            );

            helper.setText(body);

            // PDF AVEC signature (version finale)
            byte[] pdfBytes = generateSignedCertificatePdf(insurance, Language.FR);
            String fileName = "certificat-signe_" + insurance.getPolicyNumber() + ".pdf";

            helper.addAttachment(fileName, new ByteArrayResource(pdfBytes), "application/pdf");

            mailSender.send(message);
            log.info("Email de confirmation + PDF signé envoyé à {}", toEmail);

        } catch (MessagingException e) {
            log.error("Erreur envoi email confirmation", e);
        }
    }

    @Override
    public void sendActivationAndFirstPaymentEmail(Insurance insurance) {
        User user = insurance.getUser();
        String toEmail = user.getEmail();

        if (toEmail == null || toEmail.isBlank()) {
            System.out.println("WARN: Pas d'email pour l'utilisateur " + user.getId());
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Votre police AgriProtect est active ! Premier paiement requis");

            String paymentLink = "http://localhost:8081/payment.html?insuranceId=" + insurance.getId();

            String body = String.format(
                    "Cher %s %s,\n\n" +
                            "Votre police %s est signée et active depuis aujourd'hui !\n\n" +
                            "Votre couverture commence immédiatement.\n\n" +
                            "Paiement mensuel :\n" +
                            "  - Montant : %.2f TND par mois\n" +
                            "  - Premier paiement dû le : %s\n\n" +
                            "Cliquez ici pour payer maintenant : %s\n\n" +
                            "Merci de votre confiance,\nL'équipe AgriProtect",
                    user.getFirstName() != null ? user.getFirstName() : "",
                    user.getLastName() != null ? user.getLastName() : "",
                    insurance.getPolicyNumber(),
                    insurance.getAmountPerPayment(),
                    insurance.getNextPaymentDue().format(DATE_FORMAT),
                    paymentLink
            );

            helper.setText(body);
            mailSender.send(message);

            System.out.println("Email activation + paiement envoyé à " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Erreur email activation/paiement : " + e.getMessage());
        }
    }
    @Override
    public void sendContractToSignEmail(Insurance insurance) {
        User user = insurance.getUser();
        String toEmail = user.getEmail();

        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Pas d'email pour l'utilisateur {}", user.getId());
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Votre contrat AgriProtect – Veuillez signer");

            String signLink = "http://localhost:8081/sign-contract.html?id="
                    + insurance.getId() + "&token=" + insurance.getSignToken();

            String body = String.format(
                    "Cher %s %s,\n\n" +
                            "Votre police est prête !\n\n" +
                            "Pour activer votre couverture, signez le contrat via le lien suivant :\n" +
                            "%s\n\n" +
                            "Le PDF du contrat à signer est joint.\n" +
                            "Ce lien expire dans 7 jours.\n\n" +
                            "Cordialement,\nL'équipe AgriProtect",
                    user.getFirstName() != null ? user.getFirstName() : "",
                    user.getLastName() != null ? user.getLastName() : "",
                    signLink
            );

            helper.setText(body);

            // PDF SANS signature (version "à signer")
            byte[] pdfBytes = generateContractToSignPdf(insurance, Language.FR);
            String fileName = "contrat-a-signer_" + insurance.getPolicyNumber() + ".pdf";

            helper.addAttachment(fileName, new ByteArrayResource(pdfBytes), "application/pdf");

            mailSender.send(message);
            log.info("Email 'contrat à signer' envoyé à {}", toEmail);

        } catch (MessagingException e) {
            log.error("Erreur envoi email contrat à signer", e);
        }
    }
    @Override
    public void sendPaymentConfirmationEmail(Insurance insurance) {
        User user = insurance.getUser();
        String toEmail = user.getEmail();
        if (toEmail == null || toEmail.isBlank()) return;

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Confirmation de paiement - Police " + insurance.getPolicyNumber());

            String nextPaymentDate = insurance.getNextPaymentDue() != null
                    ? insurance.getNextPaymentDue().format(DATE_FORMAT)
                    : "—";

            String body = String.format(
                    "Cher %s %s,\n\n" +
                            "Nous avons bien reçu votre paiement de **%.2f TND** pour la police **%s**.\n\n" +
                            "Détails du paiement :\n" +
                            "• Police n° : %s\n" +
                            "• Montant payé : %.2f TND\n" +
                            "• Mensualités restantes : %d\n" +
                            "• Prochain paiement dû le : %s\n\n" +
                            "Votre couverture reste pleinement active.\n\n" +
                            "Merci pour votre confiance,\nL'équipe AgriProtect",

                    user.getFirstName() != null ? user.getFirstName() : "",
                    user.getLastName() != null ? user.getLastName() : "",
                    insurance.getAmountPerPayment(),
                    insurance.getPolicyNumber(),
                    insurance.getPolicyNumber(),
                    insurance.getAmountPerPayment(),
                    insurance.getRemainingPayments() != null ? insurance.getRemainingPayments() : 0,
                    insurance.getNextPaymentDue() != null ? insurance.getNextPaymentDue().format(DATE_FORMAT) : "Aucun (contrat terminé)"
            );

            helper.setText(body);
            mailSender.send(message);

            log.info("Email de confirmation envoyé à {} pour la police {}", toEmail, insurance.getPolicyNumber());

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email de confirmation", e);
        }
    }

    // PDF pour l'email "à signer" (sans signature)
    private byte[] generateContractToSignPdf(Insurance insurance, Language lang) {
        return generateBasePdf(insurance, lang, false);   // false = pas encore signé
    }

    // PDF pour l'email de confirmation (avec signature)
    private byte[] generateSignedCertificatePdf(Insurance insurance, Language lang) {
        return generateBasePdf(insurance, lang, true);    // true = signé
    }

    // Méthode commune pour éviter la duplication de code
    private byte[] generateBasePdf(Insurance insurance, Language lang, boolean isSigned) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 60, 40);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            boolean isArabic = lang.isRtl();
            if (isArabic) writer.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

            document.open();

            // Polices (identiques à avant)
            BaseFont bfBold = BaseFont.createFont("/fonts/NotoSans-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfRegular = BaseFont.createFont("/fonts/NotoSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfArabicBold = BaseFont.createFont("/fonts/NotoNaskhArabic-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfArabicRegular = BaseFont.createFont("/fonts/NotoNaskhArabic-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

            Font titleFont = new Font(isArabic ? bfArabicBold : bfBold, 22, Font.BOLD);
            Font subTitleFont = new Font(isArabic ? bfArabicRegular : bfRegular, 14, Font.NORMAL);
            Font labelFont = new Font(isArabic ? bfArabicBold : bfBold, 10, Font.BOLD);
            Font valueFont = new Font(isArabic ? bfArabicRegular : bfRegular, 10, Font.NORMAL);
            Font signFont = new Font(isArabic ? bfArabicRegular : bfRegular, 11, Font.ITALIC);

            // En-tête
            Paragraph mainTitle = new Paragraph(isSigned ? "CERTIFICAT D'ASSURANCE SIGNÉ" : "CONTRAT D'ASSURANCE - À SIGNER", titleFont);
            mainTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(mainTitle);

            Paragraph agriTitle = new Paragraph("AGRI PROTECT", subTitleFont);
            agriTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(agriTitle);
            document.add(new Paragraph("\n"));

            // Tableau des détails
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(90);
            table.setSpacingBefore(10);
            table.setSpacingAfter(20);

            String[][] rows = getLocalizedRowsForContract(insurance, lang);
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

            document.add(new Paragraph("\n\n"));

            if (isSigned) {
                // Version signée
                if (insurance.getSignatureImage() != null && insurance.getSignatureImage().length > 0) {
                    Image signImg = Image.getInstance(insurance.getSignatureImage());
                    signImg.scaleToFit(250, 80);
                    signImg.setAlignment(Element.ALIGN_CENTER);
                    document.add(signImg);
                }

                Paragraph signedInfo = new Paragraph(
                        "Signé par : " + (insurance.getSignedByName() != null ? insurance.getSignedByName() : "—") +
                                "\nDate : " + (insurance.getSignedAt() != null
                                ? insurance.getSignedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                : "—"),
                        signFont);
                signedInfo.setAlignment(Element.ALIGN_CENTER);
                document.add(signedInfo);
            } else {
                // Version à signer
                Paragraph signLine = new Paragraph("Signature de l'assuré : ______________________________", signFont);
                signLine.setAlignment(Element.ALIGN_RIGHT);
                document.add(signLine);
                document.add(new Paragraph("\nDate : ________________", signFont));
            }

            // Footer
            Paragraph footer = new Paragraph(getLocalizedFooter(lang), new Font(isArabic ? bfArabicRegular : bfRegular, 8, Font.ITALIC));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(40);
            document.add(footer);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF", e);
        }
    }
    private String[][] getLocalizedRowsForContract(Insurance insurance, Language lang) {
        boolean isAr = lang == Language.AR;
        boolean isEn = lang == Language.EN;
        User user = insurance.getUser();

        String paymentInfo = insurance.getPaymentMode() + " - "
                + insurance.getAmountPerPayment() + " TND par échéance";

        return new String[][] {
                {isAr ? "رقم الوثيقة" : (isEn ? "Policy Number" : "Numéro de police"), insurance.getPolicyNumber()},
                {isAr ? "المؤمن له" : (isEn ? "Insured" : "Assuré"), getFullName(user)},
                {isAr ? "البريد الإلكتروني" : (isEn ? "Email" : "Email"), user.getEmail() != null ? user.getEmail() : "—"},
                {isAr ? "نوع التغطية" : (isEn ? "Coverage Type" : "Type de couverture"), insurance.getCoverageType().name()},
                {isAr ? "المبلغ المؤمن عليه" : (isEn ? "Insured Amount" : "Montant assuré"), String.format("%,.2f TND", insurance.getInsuredAmount())},
                {isAr ? "الاشتراك السنوي" : (isEn ? "Annual Premium" : "Prime annuelle"), String.format("%,.2f TND", insurance.getPremiumAmount())},
                {isAr ? "طريقة الدفع" : (isEn ? "Payment Mode" : "Mode de paiement"), paymentInfo},
                {isAr ? "تاريخ البدء" : (isEn ? "Start Date" : "Date de début"), insurance.getStartDate().format(DATE_FORMAT)},
                {isAr ? "تاريخ الانتهاء" : (isEn ? "End Date" : "Date de fin"), insurance.getEndDate().format(DATE_FORMAT)},
                {isAr ? "الحالة" : (isEn ? "Status" : "Statut"), insurance.getStatus().name()}
        };
    }
    // Traductions
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
            case EN -> "AGRI PROTECT";
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
                {isAr ? "المبلغ المؤمن عليه" : (isEn ? "Insured Amount" : "Montant assuré"), String.format("%,.2f TND", insurance.getInsuredAmount() != null ? insurance.getInsuredAmount() : BigDecimal.ZERO)},
                {isAr ? "الاشتراك السنوي" : (isEn ? "Annual Premium" : "Prime annuelle"), String.format("%,.2f TND", insurance.getPremiumAmount() != null ? insurance.getPremiumAmount() : BigDecimal.ZERO)},
                {isAr ? "تاريخ البدء" : (isEn ? "Start Date" : "Date de début"), insurance.getStartDate() != null ? insurance.getStartDate().format(DATE_FORMAT) : "—"},
                {isAr ? "تاريخ الانتهاء" : (isEn ? "End Date" : "Date de fin"), insurance.getEndDate() != null ? insurance.getEndDate().format(DATE_FORMAT) : "—"},
                {isAr ? "الحالة" : (isEn ? "Status" : "Statut"), insurance.getStatus() != null ? insurance.getStatus().name() : "—"}
        };
    }

    private String getFullName(User user) {
        if (user == null) return "Non renseigné";
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "Non renseigné" : full;
    }
    // ==================== EMAILS DE RELANCE ====================

    @Override
    public void sendPaymentReminder(Insurance insurance, String daysLeft) {
        User user = insurance.getUser();
        String toEmail = user.getEmail();
        if (toEmail == null || toEmail.isBlank()) return;

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Rappel : Votre prochain paiement AgriProtect est dans " + daysLeft);

            String body = String.format(
                    "Cher %s %s,\n\n" +
                            "Nous vous rappelons que votre prochain paiement pour la police **%s** est prévu dans **%s**.\n\n" +
                            "Détails :\n" +
                            "• Montant dû : %.2f TND\n" +
                            "• Date d'échéance : %s\n\n" +
                            "Pour éviter tout retard et pénalité, nous vous invitons à régler ce montant dès maintenant.\n\n" +
                            "Cliquez ici pour payer : http://localhost:8081/payment.html?insuranceId=%s\n\n" +
                            "Merci pour votre confiance,\nL'équipe AgriProtect",

                    user.getFirstName() != null ? user.getFirstName() : "",
                    user.getLastName() != null ? user.getLastName() : "",
                    insurance.getPolicyNumber(),
                    daysLeft,
                    insurance.getAmountPerPayment(),
                    insurance.getNextPaymentDue().format(DATE_FORMAT),
                    insurance.getId()
            );

            helper.setText(body);
            mailSender.send(message);

            log.info("Email de rappel ({}) envoyé pour la police {}", daysLeft, insurance.getPolicyNumber());

        } catch (MessagingException e) {
            log.error("Erreur envoi email rappel", e);
        }
    }

// ==================== EMAIL DE PÉNALITÉ (Retard) ====================

    @Override
    public void sendOverdueNotification(Insurance insurance) {
        User user = insurance.getUser();
        String toEmail = user.getEmail();
        if (toEmail == null || toEmail.isBlank()) return;

        BigDecimal penalty = insurance.getAmountPerPayment().multiply(new BigDecimal("0.10"));
        BigDecimal totalDue = insurance.getAmountPerPayment().add(penalty);

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Retard de paiement - Pénalité appliquée - Police " + insurance.getPolicyNumber());

            String body = String.format(
                    "Cher %s %s,\n\n" +
                            "Votre paiement pour la police **%s** est en retard depuis le %s.\n\n" +
                            "Une pénalité de retard de 10%% a été appliquée.\n\n" +
                            "Détails actuels :\n" +
                            "• Montant de base : %.2f TND\n" +
                            "• Pénalité (10%%) : %.2f TND\n" +
                            "• **Total à payer maintenant** : %.2f TND\n\n" +
                            "Pour éviter la suspension de votre couverture, veuillez régulariser votre paiement dans les plus brefs délais.\n\n" +
                            "Cliquez ici pour payer : http://localhost:8081/payment.html?insuranceId=%s\n\n" +
                            "Cordialement,\nL'équipe AgriProtect",

                    user.getFirstName() != null ? user.getFirstName() : "",
                    user.getLastName() != null ? user.getLastName() : "",
                    insurance.getPolicyNumber(),
                    insurance.getNextPaymentDue().format(DATE_FORMAT),
                    insurance.getAmountPerPayment(),
                    penalty,
                    totalDue,
                    insurance.getId()
            );

            helper.setText(body);
            mailSender.send(message);

            log.info("Email de pénalité envoyé pour la police {}", insurance.getPolicyNumber());

        } catch (MessagingException e) {
            log.error("Erreur envoi email pénalité", e);
        }
    }
    @Override
    public void sendCoverageSuspendedEmail(Insurance insurance) {
        User user = insurance.getUser();
        String toEmail = user.getEmail();
        if (toEmail == null || toEmail.isBlank()) return;

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Suspension de couverture - Police " + insurance.getPolicyNumber());

            String body = String.format(
                    "Cher %s %s,\n\n" +
                            "Nous vous informons que votre police **%s** a été **suspendue** suite à un retard de paiement de plus de 15 jours.\n\n" +
                            "Pour réactiver votre couverture, veuillez régulariser tous les paiements dus dans les plus brefs délais.\n\n" +
                            "Cordialement,\nL'équipe AgriProtect",
                    user.getFirstName() != null ? user.getFirstName() : "",
                    user.getLastName() != null ? user.getLastName() : "",
                    insurance.getPolicyNumber()
            );

            helper.setText(body);
            mailSender.send(message);

            log.info("Email de suspension de couverture envoyé pour la police {}", insurance.getPolicyNumber());

        } catch (MessagingException e) {
            log.error("Erreur envoi email suspension", e);
        }
    }
    @Override
    public void sendRegularizationConfirmationEmail(Insurance insurance) {
        User user = insurance.getUser();
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("✅ Votre police AgriProtect a été réactivée - " + insurance.getPolicyNumber());

            // Correction ici : utilisation du ternaire au lieu de ||
            String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
            String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
            String fullName = (firstName + " " + lastName).trim();
            if (fullName.isEmpty()) fullName = "Cher client";

            String nextPaymentStr = insurance.getNextPaymentDue() != null
                    ? insurance.getNextPaymentDue().format(DATE_FORMAT)
                    : "Non défini";

            String body = String.format(
                    "Cher %s,\n\n" +
                            "Nous avons bien reçu votre paiement de régularisation.\n\n" +
                            "Votre police **%s** est maintenant **réactivée** avec succès.\n" +
                            "Votre couverture est à nouveau active.\n\n" +
                            "Prochain paiement prévu le : %s\n\n" +
                            "Merci pour votre confiance,\nL'équipe AgriProtect",

                    fullName,
                    insurance.getPolicyNumber(),
                    nextPaymentStr
            );

            helper.setText(body);
            mailSender.send(message);

            log.info("Email de régularisation envoyé à {}", user.getEmail());

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email de régularisation", e);
        }
    }

}