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
            System.out.println("WARN: Pas d'email pour l'utilisateur " + user.getId());
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Votre police AgriProtect est signée et active !");

            // Lien vers la page de paiement (tu peux garder le token temporaire)
            String paymentLink = "http://localhost:8081/payment.html?insuranceId=" + insurance.getId();

            // Calcul du montant et de la fréquence à afficher dans l'email
            String paymentFrequency = switch (insurance.getPaymentMode()) {
                case MONTHLY -> "mensuel";
                case QUARTERLY -> "trimestriel";
                case SEMI_ANNUAL -> "semestriel";
                case ANNUAL -> "annuel";
            };

            String body = String.format(
                    "Cher %s %s,\n\n" +
                            "Votre contrat a été signé avec succès ! Votre police est maintenant ACTIVE.\n\n" +
                            "Détails de votre police :\n" +
                            "- Numéro : %s\n" +
                            "- Type de couverture : %s\n" +
                            "- Montant assuré : %.2f TND\n" +
                            "- Prime annuelle : %.2f TND\n" +
                            "- Mode de paiement : %s\n" +
                            "- Montant par échéance : %.2f TND (%s)\n" +
                            "- Début : %s\n" +
                            "- Fin : %s\n\n" +
                            "Votre couverture commence dès aujourd'hui.\n\n" +
                            "Paiement requis pour maintenir la couverture :\n" +
                            "  - Montant : %.2f TND\n" +
                            "  - Fréquence : %s\n" +
                            "  - Premier paiement dû le : %s\n\n" +
                            "Cliquez ici pour payer maintenant : %s\n\n" +
                            "Le certificat signé est joint en PDF.\n\n" +
                            "Merci de votre confiance,\nL'équipe AgriProtect",
                    user.getFirstName() != null ? user.getFirstName() : "",
                    user.getLastName() != null ? user.getLastName() : "",
                    insurance.getPolicyNumber(),
                    insurance.getCoverageType(),
                    insurance.getInsuredAmount(),
                    insurance.getPremiumAmount(),
                    insurance.getPaymentMode(),
                    insurance.getAmountPerPayment(),
                    paymentFrequency,
                    insurance.getStartDate().format(DATE_FORMAT),
                    insurance.getEndDate().format(DATE_FORMAT),
                    insurance.getAmountPerPayment(),           // montant à payer maintenant
                    paymentFrequency,
                    insurance.getNextPaymentDue() != null
                            ? insurance.getNextPaymentDue().format(DATE_FORMAT)
                            : "—",
                    paymentLink
            );

            helper.setText(body);

            // Joint le PDF signé
            byte[] pdfBytes = generateCertificatePdf(insurance, Language.FR);
            String fileName = "certificat-signe_" + insurance.getPolicyNumber() + ".pdf";
            helper.addAttachment(fileName, new ByteArrayResource(pdfBytes), "application/pdf");

            mailSender.send(message);

            System.out.println("Email confirmation finale + PDF + lien paiement envoyé à " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Erreur email confirmation : " + e.getMessage());
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
            System.out.println("WARN: Pas d'email pour l'utilisateur " + user.getId());
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Votre contrat AgriProtect – Veuillez signer");

            String signLink = "http://localhost:8081/sign-contract.html?id="
                    + insurance.getId()
                    + "&token=" + insurance.getSignToken();

            String body = String.format(
                    "Cher %s %s,\n\n" +
                            "Votre police est prête !\n\n" +
                            "Pour activer votre couverture, signez le contrat :\n" +
                            "%s\n\n" +
                            "Le PDF du contrat est joint.\n" +
                            "Ce lien expire dans 7 jours.\n\n" +
                            "Cordialement,\nL'équipe AgriProtect",
                    user.getFirstName() != null ? user.getFirstName() : "",
                    user.getLastName() != null ? user.getLastName() : "",
                    signLink
            );

            helper.setText(body);

            byte[] pdfBytes = generateCertificatePdf(insurance, Language.FR);
            String fileName = "contrat-a-signer_" + insurance.getPolicyNumber() + ".pdf";
            helper.addAttachment(fileName, new ByteArrayResource(pdfBytes), "application/pdf");

            mailSender.send(message);
            System.out.println("Email 'contrat à signer' envoyé à " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Erreur email 'à signer' : " + e.getMessage());
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
                            "Merci pour votre confiance,\n" +
                            "L'équipe AgriProtect",

                    user.getFirstName() != null ? user.getFirstName() : "",
                    user.getLastName() != null ? user.getLastName() : "",
                    insurance.getAmountPerPayment(),
                    insurance.getPolicyNumber(),
                    insurance.getPolicyNumber(),
                    insurance.getAmountPerPayment(),
                    insurance.getRemainingPayments() != null ? insurance.getRemainingPayments() : 0,
                    nextPaymentDate
            );

            helper.setText(body);
            mailSender.send(message);

            log.info("Email de confirmation envoyé à {} pour la police {}", toEmail, insurance.getPolicyNumber());

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email de confirmation", e);
        }
    }

    private byte[] generateCertificatePdf(Insurance insurance, Language lang) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Marges plus serrées pour tout faire rentrer sur 1 page
            Document document = new Document(PageSize.A4, 40, 40, 60, 40);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            boolean isArabic = lang.isRtl();
            if (isArabic) {
                writer.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            }

            document.open();

            // Polices (tailles légèrement réduites)
            BaseFont bfBold = BaseFont.createFont("/fonts/NotoSans-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfRegular = BaseFont.createFont("/fonts/NotoSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfArabicBold = BaseFont.createFont("/fonts/NotoNaskhArabic-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfArabicRegular = BaseFont.createFont("/fonts/NotoNaskhArabic-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

            Font titleFont = new Font(isArabic ? bfArabicBold : bfBold, 22, Font.BOLD);
            Font subTitleFont = new Font(isArabic ? bfArabicRegular : bfRegular, 14, Font.NORMAL);
            Font headerFont = new Font(isArabic ? bfArabicBold : bfBold, 12, Font.BOLD);
            Font labelFont = new Font(isArabic ? bfArabicBold : bfBold, 10, Font.BOLD);
            Font valueFont = new Font(isArabic ? bfArabicRegular : bfRegular, 10, Font.NORMAL);
            Font signFont = new Font(isArabic ? bfArabicRegular : bfRegular, 11, Font.ITALIC);
            Font dateFont = new Font(isArabic ? bfArabicRegular : bfRegular, 9, Font.ITALIC);
            Font footerFont = new Font(isArabic ? bfArabicRegular : bfRegular, 8, Font.ITALIC);

            // En-tête
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);

            // Logo (optionnel, réduit)
            try {
                Image logo = Image.getInstance(getClass().getResource("/images/logo.png"));
                logo.scaleToFit(140, 140);
                logo.setAlignment(Element.ALIGN_CENTER);
                PdfPCell logoCell = new PdfPCell(logo);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                logoCell.setPaddingBottom(10);
                headerTable.addCell(logoCell);
            } catch (Exception ignored) {}

            Paragraph mainTitle = new Paragraph("CERTIFICAT D'ASSURANCE", titleFont);
            mainTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(mainTitle);

            Paragraph agriTitle = new Paragraph("AGRI PROTECT", subTitleFont);
            agriTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(agriTitle);

            document.add(new Paragraph("\n"));

            // Ligne fine
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setFixedHeight(1.5f);
            lineCell.setBorder(Rectangle.NO_BORDER);
            line.addCell(lineCell);
            document.add(line);

            document.add(new Paragraph("\n"));

            // Tableau des détails (réduction padding)
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(90);
            table.setSpacingBefore(10);
            table.setSpacingAfter(15);

            PdfPCell tableHeader = new PdfPCell(new Paragraph("Détails de la Police", headerFont));
            tableHeader.setColspan(2);
            tableHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            tableHeader.setPadding(8);
            tableHeader.setBorderWidth(0);
            table.addCell(tableHeader);

            String[][] rows = getLocalizedRows(insurance, lang);
            boolean alternate = false;
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
                alternate = !alternate;
            }

            document.add(table);

            // Signature juste après le tableau (sans saut de page)
            PdfPTable signTable = new PdfPTable(1);
            signTable.setWidthPercentage(70);
            signTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            signTable.setSpacingBefore(15); // petit espacement

            PdfPCell signCell = new PdfPCell();
            signCell.setBorder(Rectangle.NO_BORDER);
            signCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            signCell.setPadding(10);

            // Signature image
            if (insurance.getSignatureImage() != null && insurance.getSignatureImage().length > 0) {
                try {
                    Image signImg = Image.getInstance(insurance.getSignatureImage());
                    signImg.scaleToFit(280, 100); // taille réduite pour rentrer
                    signImg.setAlignment(Element.ALIGN_RIGHT);
                    signCell.addElement(signImg);
                } catch (Exception e) {
                    signCell.addElement(new Paragraph("[Signature non disponible]", signFont));
                }
            } else {
                signCell.addElement(new Paragraph("______________________________", signFont));
            }

            // Nom et date
            if (insurance.getSignedByName() != null) {
                signCell.addElement(new Paragraph("Signé par : " + insurance.getSignedByName(), signFont));
            }
            if (insurance.getSignedAt() != null) {
                signCell.addElement(new Paragraph("Date : " + insurance.getSignedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), dateFont));
            }

            signTable.addCell(signCell);
            document.add(signTable);

            // Footer (en bas)
            Paragraph footer = new Paragraph(getLocalizedFooter(lang), footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(40);
            document.add(footer);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF : " + e.getMessage(), e);
        }
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

}