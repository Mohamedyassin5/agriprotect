package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.agri.entities.Crop;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.repositories.CropRepository;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.ICropService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements ICreditService {

    private final CreditRepository creditRepository;
    private final EcheanceRepository echeanceRepository;

    @Override
    @Transactional
    public List<EcheanceResponseDto> genererEcheancier(Long creditId) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Crédit non trouvé"));

        if (!credit.getEcheances().isEmpty()) {
            throw new IllegalStateException("L'échéancier existe déjà pour ce crédit");
        }

        List<Echeance> echeances = new ArrayList<>();
        LocalDate dateCourante = credit.getDateDebut();
        double capitalRestant = credit.getMontant();
        double tauxMensuel = credit.getTauxInteret() / 100 / 12;

        double mensualite = calculerMensualite(
                credit.getMontant(),
                tauxMensuel,
                credit.getDureeMois()
        );

        for (int i = 1; i <= credit.getDureeMois(); i++) {
            double interets = capitalRestant * tauxMensuel;
            double amortissementCapital = mensualite - interets;
            double capitalApres = capitalRestant - amortissementCapital;

            Echeance echeance = Echeance.builder()
                    .numeroEcheance(i)
                    .dateEcheance(dateCourante)
                    .montantDu(mensualite)
                    .capitalDu(amortissementCapital)
                    .interetsDu(interets)
                    .assuranceDu(0.0)
                    .statut(StatutEcheance.A_VENIR)
                    .credit(credit)
                    .build();

            echeances.add(echeance);

            capitalRestant = capitalApres;
            dateCourante = dateCourante.plusMonths(1);
        }

        echeanceRepository.saveAll(echeances);
        credit.setEcheances(echeances);
        creditRepository.save(credit);

        return echeances.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private double calculerMensualite(double principal, double tauxMensuel, int nbMois) {
        if (tauxMensuel == 0) return principal / nbMois;
        return principal * (tauxMensuel * Math.pow(1 + tauxMensuel, nbMois))
                / (Math.pow(1 + tauxMensuel, nbMois) - 1);
    }

    @Override
    @Transactional
    public EcheanceResponseDto enregistrerPaiement(Long echeanceId, EcheancePaiementDto dto) {
        Echeance echeance = echeanceRepository.findById(echeanceId)
                .orElseThrow(() -> new RuntimeException("Échéance non trouvée"));

        if (echeance.getStatut() == StatutEcheance.PAYEE_A_TEMPS || 
            echeance.getStatut() == StatutEcheance.PAYEE_EN_RETARD) {
            throw new IllegalStateException("Échéance déjà payée");
        }

        double nouveauPaye = echeance.getMontantPaye() + dto.getMontantPaye();
        echeance.setMontantPaye(nouveauPaye);
        echeance.setDatePaiementEffectif(dto.getDatePaiement() != null ? dto.getDatePaiement() : LocalDate.now());

        if (nouveauPaye >= echeance.getMontantDu()) {
            if (dto.getDatePaiement().isAfter(echeance.getDateEcheance())) {
                echeance.setStatut(StatutEcheance.PAYEE_EN_RETARD);
            } else {
                echeance.setStatut(StatutEcheance.PAYEE_A_TEMPS);
            }
        } else if (nouveauPaye > 0) {
            echeance.setStatut(StatutEcheance.PAYEE_PARTIELLEMENT);
        }

        updateCreditAfterPayment(echeance.getCredit());

        echeanceRepository.save(echeance);
        return mapToResponseDto(echeance);
    }

}