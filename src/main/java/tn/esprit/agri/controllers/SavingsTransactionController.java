package tn.esprit.agri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.dto_savings_accountability.TransactionRequest;
import tn.esprit.agri.dto_savings_accountability.TransactionResponse;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.SavingsTransactionType;
import tn.esprit.agri.services.ISavingsTransactionService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/savings/transactions")
@RequiredArgsConstructor
public class SavingsTransactionController {

    private final ISavingsTransactionService transactionService;

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<TransactionResponse> createTransaction(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TransactionRequest request) {

        TransactionResponse response = transactionService.createTransaction(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<Page<TransactionResponse>> listTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) SavingsTransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("occurredAt").descending());
        Page<TransactionResponse> response = transactionService.getTransactions(user.getId(), type, from, to, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<TransactionResponse> getTransaction(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        TransactionResponse response = transactionService.getTransaction(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTransaction(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        transactionService.deleteTransaction(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
