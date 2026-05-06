package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.DTO.wallet.*;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.IWalletService;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WalletController {

    private final IWalletService walletService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(Authentication auth) {
        return ResponseEntity.ok(walletService.getOrCreateWallet(getUserId(auth)));
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> deposit(@RequestBody WalletDepositRequest req, Authentication auth) {
        return ResponseEntity.ok(walletService.deposit(getUserId(auth), req));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WalletResponse> withdraw(@RequestBody WalletDepositRequest req, Authentication auth) {
        return ResponseEntity.ok(walletService.withdraw(getUserId(auth), req));
    }

    @PostMapping("/transfer")
    public ResponseEntity<WalletResponse> transfer(@RequestBody WalletTransferRequest req, Authentication auth) {
        // Résolution email → userId si recipientUserId absent
        if ((req.getRecipientUserId() == null || req.getRecipientUserId().isBlank())
                && req.getRecipientEmail() != null && !req.getRecipientEmail().isBlank()) {
            String recipientId = userRepository.findByEmail(req.getRecipientEmail())
                    .orElseThrow(() -> new RuntimeException("Aucun compte trouvé pour l'email : " + req.getRecipientEmail()))
                    .getId();
            req.setRecipientUserId(recipientId);
        }
        return ResponseEntity.ok(walletService.transfer(getUserId(auth), req));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<WalletTransactionResponse>> transactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        return ResponseEntity.ok(walletService.getTransactions(getUserId(auth),
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PutMapping("/emergency/config")
    public ResponseEntity<WalletResponse> configureEmergency(@RequestBody EmergencyConfigRequest req, Authentication auth) {
        return ResponseEntity.ok(walletService.configureEmergencyFund(getUserId(auth), req));
    }

    @PostMapping("/emergency/topup")
    public ResponseEntity<WalletResponse> topUpEmergency(@RequestBody EmergencyFundTopUpRequest req, Authentication auth) {
        return ResponseEntity.ok(walletService.topUpEmergencyFund(getUserId(auth), req));
    }

    @PostMapping("/emergency/withdraw")
    public ResponseEntity<EmergencyWithdrawalResponse> requestEmergencyWithdrawal(
            @RequestBody EmergencyWithdrawalRequest req, Authentication auth) {
        return ResponseEntity.ok(walletService.requestEmergencyWithdrawal(getUserId(auth), req));
    }

    @GetMapping("/emergency/withdrawals")
    public ResponseEntity<List<EmergencyWithdrawalResponse>> myWithdrawals(Authentication auth) {
        return ResponseEntity.ok(walletService.getEmergencyWithdrawals(getUserId(auth)));
    }

    private String getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }
}
