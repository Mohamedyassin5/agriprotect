package tn.esprit.agri.controlleurs;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.DTO.DemandeDTO.CreationDemandeCreditDto;
import tn.esprit.agri.DTO.DemandeDTO.DemandeCreditResponseDto;
import tn.esprit.agri.DTO.DemandeDTO.UpdateDemandeCreditDto;
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
    public ResponseEntity<List<DemandeCreditResponseDto>> getAllDemandes() {
        return ResponseEntity.ok(demandeCreditService.getAllDemandes());
    }
}

