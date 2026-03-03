package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final ICreditService creditService;

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
}