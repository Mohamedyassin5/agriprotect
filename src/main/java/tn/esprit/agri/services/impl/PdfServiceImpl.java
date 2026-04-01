package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.Payment;
import tn.esprit.agri.services.PdfService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    @Override
    public byte[] generateInvoice(Insurance insurance, Payment payment) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            // ====================== EN-TÊTE ======================
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("FACTURE D'ASSURANCE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
            Paragraph subtitle = new Paragraph("AgriProtect - Protection de vos cultures", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(new Paragraph(" ")); // espace

            // ====================== INFORMATIONS CLIENT & POLICE ======================
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1.5f, 2.5f});

            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);

            String fullName = insurance.getUser().getFirstName() + " "
                    + (insurance.getUser().getLastName() != null ? insurance.getUser().getLastName() : "");

            addTableRow(infoTable, "Numéro de police :", insurance.getPolicyNumber(), bold, normal);
            addTableRow(infoTable, "Assuré :", fullName, bold, normal);
            addTableRow(infoTable, "Email :", insurance.getUser().getEmail(), bold, normal);

            if (insurance.getUser().getPhoneNumber() != null && !insurance.getUser().getPhoneNumber().isEmpty()) {
                addTableRow(infoTable, "Téléphone :", insurance.getUser().getPhoneNumber(), bold, normal);
            }

            addTableRow(infoTable, "Date d'émission :", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), bold, normal);

            document.add(infoTable);
            document.add(new Paragraph(" "));

            // ====================== DÉTAILS DU PAIEMENT (TABLEAU) ======================
            PdfPTable paymentTable = new PdfPTable(2);
            paymentTable.setWidthPercentage(100);
            paymentTable.setWidths(new float[]{2f, 1f});

            // En-tête du tableau
            PdfPCell header1 = new PdfPCell(new Phrase("Description", bold));
            header1.setBackgroundColor(BaseColor.LIGHT_GRAY);
            header1.setHorizontalAlignment(Element.ALIGN_CENTER);
            paymentTable.addCell(header1);

            PdfPCell header2 = new PdfPCell(new Phrase("Montant (DT)", bold));
            header2.setBackgroundColor(BaseColor.LIGHT_GRAY);
            header2.setHorizontalAlignment(Element.ALIGN_CENTER);
            paymentTable.addCell(header2);

            // Lignes
            addPaymentRow(paymentTable, "Prime d'assurance (" + insurance.getCoverageType() + ")",
                    insurance.getPremiumAmount(), normal);

            if (payment.getPenaltyAmount() != null && payment.getPenaltyAmount().compareTo(BigDecimal.ZERO) > 0) {
                addPaymentRow(paymentTable, "Pénalité de retard", payment.getPenaltyAmount(), normal);
            }

            // Total
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
            PdfPCell totalCell1 = new PdfPCell(new Phrase("TOTAL PAYÉ", totalFont));
            totalCell1.setHorizontalAlignment(Element.ALIGN_RIGHT);
            paymentTable.addCell(totalCell1);

            PdfPCell totalCell2 = new PdfPCell(new Phrase(payment.getAmount() + " DT", totalFont));
            totalCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            paymentTable.addCell(totalCell2);

            document.add(paymentTable);

            document.add(new Paragraph(" "));

            // ====================== PIED DE PAGE ======================
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Paragraph footer = new Paragraph("Merci pour votre confiance ! Nous protégeons vos cultures avec sérieux et engagement.", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            Paragraph thanks = new Paragraph("AgriProtect © " + LocalDate.now().getYear() + " - Tous droits réservés", footerFont);
            thanks.setAlignment(Element.ALIGN_CENTER);
            document.add(thanks);

            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de la génération de la facture PDF pour la police {}", insurance.getPolicyNumber(), e);
            throw new RuntimeException("Impossible de générer la facture PDF", e);
        }
    }
    private void addTableRow(PdfPTable table, String label, String value, Font boldFont, Font normalFont) {
        table.addCell(new PdfPCell(new Phrase(label, boldFont)));
        table.addCell(new PdfPCell(new Phrase(value, normalFont)));
    }

    private void addPaymentRow(PdfPTable table, String description, BigDecimal amount, Font font) {
        table.addCell(new PdfPCell(new Phrase(description, font)));
        table.addCell(new PdfPCell(new Phrase(amount != null ? amount.toString() : "0", font)));
    }
    }
