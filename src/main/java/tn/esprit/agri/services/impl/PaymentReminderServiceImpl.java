package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.enums.InsuranceStatus;
import tn.esprit.agri.repositories.InsuranceRepository;
import tn.esprit.agri.services.EmailService;
import tn.esprit.agri.services.PaymentReminderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReminderServiceImpl implements PaymentReminderService {

    private final InsuranceRepository insuranceRepository;
    private final EmailService emailService;

    /**
     * Exécuté tous les jours à 9h00
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    @Override
    public void checkAndSendReminders() {
        LocalDate today = LocalDate.now();
        log.info("Début du check quotidien des rappels et pénalités...");

        List<Insurance> activeInsurances = insuranceRepository.findByStatus(InsuranceStatus.ACTIVE);

        int processed = 0;

        for (Insurance insurance : activeInsurances) {
            if (insurance.getNextPaymentDue() == null) continue;

            long daysUntilDue = ChronoUnit.DAYS.between(today, insurance.getNextPaymentDue());

            try {
                // 1. Rappel 2 jours avant
                if (daysUntilDue == 2) {
                    emailService.sendPaymentReminder(insurance, "2 jours");
                    log.info("Relance 2 jours envoyée pour la police {}", insurance.getPolicyNumber());
                    processed++;
                }

                // 2. Pénalité après 7 jours de retard
                else if (daysUntilDue <= -7 && !Boolean.TRUE.equals(insurance.isOverdue())) {
                    applyPenalty(insurance);
                    processed++;
                }

                // 3. Suspension après 15 jours de retard
                else if (daysUntilDue <= -15 && insurance.getStatus() != InsuranceStatus.SUSPENDED) {
                    suspendCoverage(insurance);
                    processed++;
                }

            } catch (Exception e) {
                log.error("Erreur lors du traitement de la police {}", insurance.getPolicyNumber(), e);
            }
        }

        log.info("Check quotidien terminé. {} assurances traitées.", processed);
    }

    /**
     * Applique la pénalité de 10% après 7 jours de retard
     */
    private void applyPenalty(Insurance insurance) {
        BigDecimal penaltyRate = new BigDecimal("0.10");
        BigDecimal penaltyAmount = insurance.getAmountPerPayment().multiply(penaltyRate);

        insurance.setPenaltyAmount(insurance.getPenaltyAmount().add(penaltyAmount));
        insurance.setOverdue(true);
        insurance.setStatus(InsuranceStatus.OVERDUE);

        insuranceRepository.save(insurance);

        emailService.sendOverdueNotification(insurance);

        log.warn("Pénalité de 10% appliquée sur la police {} | Pénalité : {} TND",
                insurance.getPolicyNumber(), penaltyAmount);
    }

    /**
     * Suspend la couverture après 15 jours de retard
     */
    private void suspendCoverage(Insurance insurance) {
        insurance.setStatus(InsuranceStatus.SUSPENDED);   // ou CANCELLED selon ton enum
        insurance.setOverdue(true);

        insuranceRepository.save(insurance);

        emailService.sendCoverageSuspendedEmail(insurance);   // À créer dans EmailService

        log.error("Couverture SUSPENDUE pour la police {} (15 jours de retard)",
                insurance.getPolicyNumber());
    }
}