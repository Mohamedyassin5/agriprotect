package tn.esprit.agri.controlleurs;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.DTO.CreditDTO.CreditSimulationRequestDto;
import tn.esprit.agri.DTO.CreditDTO.CreditSimulationResponseDto;
import tn.esprit.agri.DTO.CreditDTO.PortfolioAlertDto;
import tn.esprit.agri.DTO.CreditDTO.PortfolioKpiDto;
import tn.esprit.agri.DTO.EcheanceDTO.EcheancePaiementDto;
import tn.esprit.agri.DTO.EcheanceDTO.EcheanceResponseDto;
import tn.esprit.agri.services.ICreditService;
import tn.esprit.agri.services.PortfolioAnalyticsService;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final ICreditService creditService;
    private final PortfolioAnalyticsService portfolioAnalyticsService;

    @PostMapping("/{creditId}/echeancier")
    public ResponseEntity<List<EcheanceResponseDto>> genererEcheancier(@PathVariable Long creditId) {
        return ResponseEntity.ok(creditService.genererEcheancier(creditId));
    }

    @GetMapping("/{creditId}/echeances")
    public ResponseEntity<List<EcheanceResponseDto>> getEcheances(@PathVariable Long creditId) {
        return ResponseEntity.ok(creditService.getEcheancesByCredit(creditId));
    }

    @PostMapping("/echeances/{echeanceId}/paiement")
    public ResponseEntity<EcheanceResponseDto> enregistrerPaiement(
            @PathVariable Long echeanceId,
            @Valid @RequestBody EcheancePaiementDto dto) {
        return ResponseEntity.ok(creditService.enregistrerPaiement(echeanceId, dto));
    }

    @GetMapping("/{creditId}/echeances/upcoming")
    public ResponseEntity<List<EcheanceResponseDto>> getUpcoming(@PathVariable Long creditId) {
        return ResponseEntity.ok(creditService.getUpcomingEcheances(creditId));
    }

    @GetMapping("/{creditId}/echeances/overdue")
    public ResponseEntity<List<EcheanceResponseDto>> getOverdue(@PathVariable Long creditId) {
        return ResponseEntity.ok(creditService.getOverdueEcheances(creditId));
    }

    @GetMapping("/{creditId}/echeances/paid")
    public ResponseEntity<List<EcheanceResponseDto>> getPaid(@PathVariable Long creditId) {
        return ResponseEntity.ok(creditService.getPaidEcheances(creditId));
    }

    @PostMapping("/echeances/mark-overdue")
    public ResponseEntity<Integer> markOverdue() {
        return ResponseEntity.ok(creditService.markOverdueEcheances());
    }

    @PostMapping("/simulate")
    public ResponseEntity<CreditSimulationResponseDto> simulate(@Valid @RequestBody CreditSimulationRequestDto request) {
        return ResponseEntity.ok(portfolioAnalyticsService.simulateOffers(request));
    }

    @GetMapping("/portfolio/kpis")
    public ResponseEntity<PortfolioKpiDto> portfolioKpis() {
        return ResponseEntity.ok(portfolioAnalyticsService.computeKpis());
    }

    @GetMapping("/portfolio/alerts")
    public ResponseEntity<List<PortfolioAlertDto>> portfolioAlerts() {
        return ResponseEntity.ok(portfolioAnalyticsService.computeAlerts());
    }
}