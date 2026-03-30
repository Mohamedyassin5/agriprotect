package tn.esprit.agri.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.controlleurs.auth.dto.ResetPasswordRequest;
import tn.esprit.agri.entities.PasswordResetCode;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.repositories.PasswordResetCodeRepository;
import tn.esprit.agri.repositories.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthPasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository resetRepo;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private static final SecureRandom RNG = new SecureRandom();

    @Transactional
    public void forgotPassword(String email) {
        var userOpt = userRepository.findByEmail(email);

        // Security: do not reveal existence
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        int expiryMinutes = 10;
        String code = String.format("%06d", RNG.nextInt(1_000_000));

        resetRepo.deleteByEmail(email);

        resetRepo.save(PasswordResetCode.builder()
                .email(email)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES))
                .used(false)
                .build());

        String fullName = ((user.getFirstName() == null) ? "" : user.getFirstName()) + " " +
                          ((user.getLastName() == null) ? "" : user.getLastName());

        String name = fullName.trim().isEmpty() ? "Utilisateur" : fullName.trim();

        mailService.sendResetCode(user.getEmail(), name, code, expiryMinutes);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmNewPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        PasswordResetCode prc = resetRepo.findTopByEmailOrderByExpiresAtDesc(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Code invalide"));

        if (prc.isUsed()) throw new RuntimeException("Code déjà utilisé");
        if (Instant.now().isAfter(prc.getExpiresAt())) throw new RuntimeException("Code expiré");
        if (!passwordEncoder.matches(req.getCode(), prc.getCodeHash()))
            throw new RuntimeException("Code invalide");

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        prc.setUsed(true);
        resetRepo.save(prc);
    }
}
