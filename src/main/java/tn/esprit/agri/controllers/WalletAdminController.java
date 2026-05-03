package tn.esprit.agri.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.DTO.wallet.*;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.IWalletService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/wallet")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class WalletAdminController {

    private final IWalletService walletService;
    private final UserRepository userRepository;

    @GetMapping("/overview")
    public ResponseEntity<WalletAdminOverview> overview() {
        return ResponseEntity.ok(walletService.getAdminOverview());
    }

    @GetMapping("/emergency/pending")
    public ResponseEntity<List<EmergencyWithdrawalResponse>> pending() {
        return ResponseEntity.ok(walletService.getPendingWithdrawals());
    }

    @PostMapping("/emergency/{id}/approve")
    public ResponseEntity<EmergencyWithdrawalResponse> approve(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String note,
            Authentication auth) {
        String adminId = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found")).getId();
        return ResponseEntity.ok(walletService.approveWithdrawal(id, adminId, note));
    }

    @PostMapping("/emergency/{id}/reject")
    public ResponseEntity<EmergencyWithdrawalResponse> reject(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String note,
            Authentication auth) {
        String adminId = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found")).getId();
        return ResponseEntity.ok(walletService.rejectWithdrawal(id, adminId, note));
    }
}
