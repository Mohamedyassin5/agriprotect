package tn.esprit.agri.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.esprit.agri.entities.Risque;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskDetectionScheduler {

    private final IRiskDetectionService riskDetectionService;

    /**
     * Scheduled task : Détection automatique des risques toutes les heures
     * Lundi à vendredi, 6h-22h (horaires agricoles)
     */
    @Scheduled(fixedRate = 3600000) // 3600000 ms = 1 heure
    public void detectRisksScheduled() {
        log.info("=== Starting scheduled risk detection ===");
        try {
            List<Risque> detected = riskDetectionService.detectAllCropsRisks();
            log.info("Risk detection completed. {} risks detected.", detected.size());
        } catch (Exception e) {
            log.error("Error in scheduled risk detection: ", e);
        }
    }

    /**
     * Alternative : Cron expression pour une plage horaire spécifique
     * Tous les jours à 6h, 12h, 18h (trois fois par jour)
     * Cron: second, minute, hour, day of month, month, day of week
     */
    @Scheduled(cron = "0 0 6,12,18 * * *", zone = "Africa/Tunis")
    public void detectRisksScheduledAtSpecificTimes() {
        log.info("=== Starting scheduled risk detection at specific times ===");
        try {
            List<Risque> detected = riskDetectionService.detectAllCropsRisks();
            log.info("Risk detection completed. {} risks detected.", detected.size());
        } catch (Exception e) {
            log.error("Error in scheduled risk detection: ", e);
        }
    }
}
