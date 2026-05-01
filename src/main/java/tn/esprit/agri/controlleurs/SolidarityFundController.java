package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.DTO.SolidarityFundResponse;
import tn.esprit.agri.entities.SolidarityFund;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.services.impl.SolidarityFundService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/solidarity-funds")
@RequiredArgsConstructor
public class SolidarityFundController {

    private final SolidarityFundService solidarityFundService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SolidarityFundResponse> createFund(
            @RequestBody SolidarityFund fund,
            Authentication authentication) {

        User admin = (User) authentication.getPrincipal();
        SolidarityFund created = solidarityFundService.createFund(fund, admin);

        return new ResponseEntity<>(
                solidarityFundService.mapToResponse(created),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SolidarityFundResponse>> getAllFunds() {
        List<SolidarityFundResponse> response = solidarityFundService.getAllFunds()
                .stream()
                .map(solidarityFundService::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SolidarityFundResponse> getFundById(@PathVariable String id) {
        return solidarityFundService.getFundById(id)
                .map(fund -> ResponseEntity.ok(solidarityFundService.mapToResponse(fund)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFund(@PathVariable String id) {
        solidarityFundService.deleteFund(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SolidarityFundResponse> updateFund(
            @PathVariable String id,
            @RequestBody SolidarityFund fundDetails) {

        Optional<SolidarityFund> existingFundOpt = solidarityFundService.getFundById(id);
        if (existingFundOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SolidarityFund existingFund = existingFundOpt.get();
        existingFund.setName(fundDetails.getName());
        existingFund.setNumeroFond(fundDetails.getNumeroFond());
        existingFund.setCultureType(fundDetails.getCultureType());
        existingFund.setMinScore(fundDetails.getMinScore());
        existingFund.setPrimeAmount(fundDetails.getPrimeAmount());
        existingFund.setCurrentBalance(fundDetails.getCurrentBalance());

        solidarityFundService.save(existingFund);

        return ResponseEntity.ok(solidarityFundService.mapToResponse(existingFund));
    }

    @PostMapping("/{fundId}/pay/{farmerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> payPrime(
            @PathVariable String fundId,
            @PathVariable String farmerId,
            Authentication authentication) {

        User admin = (User) authentication.getPrincipal();
        solidarityFundService.payPrime(fundId, farmerId, admin);

        return ResponseEntity.ok("Paiement mensuel enregistré pour " + farmerId);
    }

    @PostMapping("/{fundId}/pay")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<String> payFundAsFarmer(
            @PathVariable String fundId,
            Authentication authentication) {

        User farmer = (User) authentication.getPrincipal();
        solidarityFundService.payPrime(fundId, farmer.getId(), farmer);

        return ResponseEntity.ok("Paiement mensuel effectué pour le fonds " + fundId);
    }

    @PostMapping("/{fundId}/join")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<String> joinFund(
            @PathVariable String fundId,
            Authentication authentication) {

        User farmer = (User) authentication.getPrincipal();
        solidarityFundService.joinFund(fundId, farmer);

        return ResponseEntity.ok("Vous avez rejoint le fonds " + fundId + " avec succès.");
    }
}
