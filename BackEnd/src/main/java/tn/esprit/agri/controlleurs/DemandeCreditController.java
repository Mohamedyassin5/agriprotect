package tn.esprit.agri.controlleurs;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.DTO.AnalyseDTO.AnalyseRentabiliteCreateDto;
import tn.esprit.agri.DTO.AnalyseDTO.AnalyseRentabiliteResponseDto;
import tn.esprit.agri.DTO.AnalyseDTO.CreditScoringDto;
import tn.esprit.agri.DTO.AnalyseDTO.DemandeAnalysisReportDto;
import tn.esprit.agri.DTO.CreditDTO.CreationCreditDto;
import tn.esprit.agri.DTO.CreditDTO.CreditResponseDto;
import tn.esprit.agri.DTO.DemandeDTO.CreationDemandeCreditDto;
import tn.esprit.agri.DTO.DemandeDTO.DecisionFinaleDto;
import tn.esprit.agri.DTO.DemandeDTO.DemandeCreditFilterDto;
import tn.esprit.agri.DTO.DemandeDTO.DemandeCreditResponseDto;
import tn.esprit.agri.DTO.DemandeDTO.UpdateDemandeCreditDto;
import tn.esprit.agri.entities.enums.StatutDemande;
import tn.esprit.agri.services.IDemandeCreditService;

import java.util.List;

@RestController
@RequestMapping("/api/demandes-credit")
@RequiredArgsConstructor
public class DemandeCreditController {

    private final IDemandeCreditService demandeCreditService;

    @PostMapping
    public ResponseEntity<DemandeCreditResponseDto> creerDemande(
            @Valid @RequestBody CreationDemandeCreditDto dto) {
        DemandeCreditResponseDto response = demandeCreditService.creerDemande(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandeCreditResponseDto> getDemande(@PathVariable Long id) {
        return ResponseEntity.ok(demandeCreditService.getDemandeById(id));
    }

    @GetMapping("/agriculteur/{agriculteurId}")
    public ResponseEntity<List<DemandeCreditResponseDto>> getByAgriculteur(
            @PathVariable Long agriculteurId) {
        return ResponseEntity.ok(demandeCreditService.getDemandesByAgriculteur(agriculteurId));
    }

    @GetMapping("/en-cours")
    public ResponseEntity<List<DemandeCreditResponseDto>> getDemandesEnCours() {
        return ResponseEntity.ok(demandeCreditService.getDemandesEnCours());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DemandeCreditResponseDto> updateDemande(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDemandeCreditDto dto) {
        return ResponseEntity.ok(demandeCreditService.updateDemande(id, dto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDemande(@PathVariable Long id) {
        demandeCreditService.deleteDemande(id);
    }

    @GetMapping
    public ResponseEntity<List<DemandeCreditResponseDto>> getAllDemandes(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) StatutDemande statut,
            @RequestParam(required = false) java.time.LocalDate dateFrom,
            @RequestParam(required = false) java.time.LocalDate dateTo) {
        DemandeCreditFilterDto filter = new DemandeCreditFilterDto();
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setDirection(direction);
        filter.setStatut(statut);
        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);
        return ResponseEntity.ok(demandeCreditService.getDemandesFiltered(filter));
    }


    @PostMapping("/{demandeId}/analyse-rentabilite")
    public ResponseEntity<AnalyseRentabiliteResponseDto> ajouterAnalyse(
            @PathVariable Long demandeId,
            @Valid @RequestBody AnalyseRentabiliteCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(demandeCreditService.creerAnalyseRentabilite(demandeId, dto));
    }

    @GetMapping("/{demandeId}/analyse-rentabilite")
    public ResponseEntity<AnalyseRentabiliteResponseDto> getAnalyse(
            @PathVariable Long demandeId) {
        return ResponseEntity.ok(demandeCreditService.getAnalyseByDemandeId(demandeId));
    }

    @PutMapping("/analyse-rentabilite/{analyseId}")
    public ResponseEntity<AnalyseRentabiliteResponseDto> updateAnalyse(
            @PathVariable Long analyseId,
            @Valid @RequestBody AnalyseRentabiliteCreateDto dto) {
        return ResponseEntity.ok(demandeCreditService.updateAnalyseRentabilite(analyseId, dto));
    }

    @PostMapping("/{demandeId}/credit")
    public ResponseEntity<CreditResponseDto> octroyerCredit(
            @PathVariable Long demandeId,
            @Valid @RequestBody CreationCreditDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(demandeCreditService.creerCreditDepuisDemande(demandeId, dto));
    }

    @GetMapping("/credit/{creditId}")
    public ResponseEntity<CreditResponseDto> getCredit(@PathVariable Long creditId) {
        return ResponseEntity.ok(demandeCreditService.getCreditById(creditId));
    }

    @GetMapping("/{demandeId}/credit")
    public ResponseEntity<CreditResponseDto> getCreditByDemande(@PathVariable Long demandeId) {
        return ResponseEntity.ok(demandeCreditService.getCreditByDemandeId(demandeId));
    }

    @PostMapping("/{demandeId}/start-instruction")
    public ResponseEntity<DemandeCreditResponseDto> startInstruction(
            @PathVariable Long demandeId,
            @RequestParam(required = false) Long actorId) {
        return ResponseEntity.ok(demandeCreditService.startInstruction(demandeId, actorId));
    }

    @PostMapping("/{demandeId}/finalize")
    public ResponseEntity<DemandeCreditResponseDto> finaliserDecision(
            @PathVariable Long demandeId,
            @Valid @RequestBody DecisionFinaleDto dto) {
        return ResponseEntity.ok(demandeCreditService.finaliserDecision(demandeId, dto));
    }

    @PostMapping("/{demandeId}/archive")
    public ResponseEntity<DemandeCreditResponseDto> archiveDemande(
            @PathVariable Long demandeId,
            @RequestParam(required = false) Long actorId) {
        return ResponseEntity.ok(demandeCreditService.archiveDemande(demandeId, actorId));
    }

    @PostMapping("/{demandeId}/cancel")
    public ResponseEntity<DemandeCreditResponseDto> cancelDemande(
            @PathVariable Long demandeId,
            @RequestParam(required = false) Long actorId) {
        return ResponseEntity.ok(demandeCreditService.cancelDemande(demandeId, actorId));
    }

    @GetMapping("/{demandeId}/score")
    public ResponseEntity<CreditScoringDto> scoreDemande(@PathVariable Long demandeId) {
        return ResponseEntity.ok(demandeCreditService.scoreDemande(demandeId));
    }

    @GetMapping("/{demandeId}/report")
    public ResponseEntity<DemandeAnalysisReportDto> reportDemande(@PathVariable Long demandeId) {
        return ResponseEntity.ok(demandeCreditService.buildDemandeReport(demandeId));
    }

}

