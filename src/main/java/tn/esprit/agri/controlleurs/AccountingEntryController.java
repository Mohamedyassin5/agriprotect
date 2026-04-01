package tn.esprit.agri.controlleurs;

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
import tn.esprit.agri.dto_savings_accountability.EntryRequest;
import tn.esprit.agri.dto_savings_accountability.EntryResponse;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.EntryCategory;
import tn.esprit.agri.entities.enums.EntryType;
import tn.esprit.agri.services.IAccountingEntryService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/accounting/entries")
@RequiredArgsConstructor
public class AccountingEntryController {

    private final IAccountingEntryService entryService;

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<EntryResponse> createEntry(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody EntryRequest request) {

        EntryResponse response = entryService.create(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<Page<EntryResponse>> listEntries(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) EntryType type,
            @RequestParam(required = false) EntryCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("entryDate").descending());
        Page<EntryResponse> response = entryService.getEntries(user.getId(), type, category, from, to, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<EntryResponse> getEntry(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        EntryResponse response = entryService.getById(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<EntryResponse> updateEntry(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody EntryRequest request) {

        EntryResponse response = entryService.update(id, user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<Void> deleteEntry(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        entryService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
