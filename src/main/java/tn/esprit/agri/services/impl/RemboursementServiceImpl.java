package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.DTO.RemboursementDTO;
import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.Remboursement;
import tn.esprit.agri.entities.SavingsAccount;
import tn.esprit.agri.entities.SavingsTransaction;
import tn.esprit.agri.entities.Sinistre;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.InsuranceStatus;
import tn.esprit.agri.entities.enums.Role;
import tn.esprit.agri.entities.enums.SavingsAccountStatus;
import tn.esprit.agri.entities.enums.SavingsTransactionType;
import tn.esprit.agri.entities.enums.StatutRemboursement;
import tn.esprit.agri.entities.enums.TypeSinistre;
import tn.esprit.agri.repositories.InsuranceRepository;
import tn.esprit.agri.repositories.RemboursementRepository;
import tn.esprit.agri.repositories.SavingsAccountRepository;
import tn.esprit.agri.repositories.SavingsTransactionRepository;
import tn.esprit.agri.repositories.SinistreRepository;
import tn.esprit.agri.services.IRemboursementService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RemboursementServiceImpl implements IRemboursementService {

    private final InsuranceRepository insuranceRepository;
    private final SinistreRepository sinistreRepository;
    private final RemboursementRepository remboursementRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsTransactionRepository savingsTransactionRepository;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTES MÉTIER
    // ═══════════════════════════════════════════════════════════════════════════

    private static final Set<TypeSinistre> TYPES_COUVERTS = Set.of(
            TypeSinistre.INONDATION,
            TypeSinistre.SECHERESSE,
            TypeSinistre.SEISME
    );

    @Value("${remboursement.seuil-auto-versement:500}")
    private BigDecimal SEUIL_AUTO_VERSEMENT;



    // FIX 4 — deux seuils : alerte (non bloquant) + blocage (bloquant)
    private static final int RATIO_ALERTE_TOTAL_PRIMES      = 15;
    private static final int RATIO_BLOQUANT_TOTAL_PRIMES    = 50;

    private static final int JOURS_CARENCE                  = 30;
    private static final int MAX_REMBOURSEMENTS_PAR_AN      = 2;

    // FIX 2 — délai de carence post-remboursement
    private static final int MOIS_CARENCE_POST_REMBOURSEMENT = 4;

    // FIX 2 — taux de pénalité si résiliation dans les 6 mois suivant un remb.
    private static final BigDecimal TAUX_PENALITE_RESILIATION = BigDecimal.valueOf(0.30);
    private static final int        MOIS_FENETRE_PENALITE      = 6;

    // ═══════════════════════════════════════════════════════════════════════════
    // CODES D'ERREUR MÉTIER
    // ═══════════════════════════════════════════════════════════════════════════

    private static final class ErreurMetier {

        // Sinistre
        static final String SINISTRE_INTROUVABLE        = "Sinistre introuvable.";
        static final String SINISTRE_NON_AUTORISE       = "Accès non autorisé : ce sinistre ne vous appartient pas.";
        static final String SINISTRE_TYPE_NON_COUVERT   = "Type de sinistre '%s' non couvert par votre contrat. "
                + "Risques pris en charge : INONDATION, SÉCHERESSE, SÉISME.";
        static final String SINISTRE_DOUBLON            = "Une demande de remboursement a déjà été soumise pour ce sinistre.";
        static final String SINISTRE_QUOTA_MANQUANT     = "L'expertise terrain de ce sinistre n'a pas encore été réalisée.";
        static final String SINISTRE_QUOTA_INVALIDE     = "Quota d'indemnisation invalide (%.1f%%). Valeur attendue : entre 1%% et 100%%.";
        static final String SINISTRE_HORS_PERIODE       = "Le sinistre est survenu en dehors de la période de validité du contrat.";

        // Assurance
        static final String ASSURANCE_INTROUVABLE       = "Aucun contrat d'assurance actif trouvé pour votre compte.";
        static final String PRIMES_EN_RETARD            = "Votre contrat présente des primes impayées. "
                + "Régularisez vos paiements avant de soumettre une demande.";
        static final String MOIS_PAYES_INSUFFISANTS     = "Éligibilité insuffisante : %d mois payé(s) sur %d requis.";
        static final String CARENCE_NON_ECOULEE         = "Le sinistre est survenu pendant la période de carence (%d jours). "
                + "Date du sinistre déclarée : %s.";

        // FIX 4 — nouveau message pour le blocage dur
        static final String MONTANT_ASSURE_BLOQUANT     = "Remboursement bloqué : montant assuré (%.2f TND) "
                + "disproportionné par rapport aux primes versées (%.2f TND). "
                + "Ratio constaté : %.1fx (seuil bloquant : %dx). "
                + "Contactez votre conseiller pour réviser votre contrat.";

        // Fréquence
        static final String FREQUENCE_MAX_ATTEINTE      = "Vous avez déjà perçu %d remboursement(s) au cours "
                + "des 12 derniers mois (limite : %d par an).";

        // FIX 5 — carence post-remboursement
        static final String CARENCE_POST_REMBOURSEMENT  = "Nouveau remboursement impossible avant le %s "
                + "(%d mois de carence requis après le dernier versement du %s).";

        // Calcul financier
        static final String DOMMAGES_SOUS_FRANCHISE     = "Les dommages estimés (%.2f TND) sont inférieurs "
                + "ou égaux à la franchise du contrat (%.2f TND).";
        static final String MONTANT_NET_NUL             = "Aucune indemnisation possible : après déduction des primes "
                + "restantes (%.2f TND), le montant net est nul ou négatif.";

        // Remboursement
        static final String REMBOURSEMENT_INTROUVABLE               = "Demande de remboursement introuvable.";
        static final String REMBOURSEMENT_NON_AUTORISE               = "Accès refusé : cette demande de remboursement ne vous appartient pas.";
        static final String REMBOURSEMENT_STATUT_INVALIDE_APPROBATION = "Cette demande ne peut pas être approuvée (statut actuel : %s). "
                + "Seules les demandes EN_ATTENTE sont approuvables.";
        static final String REMBOURSEMENT_STATUT_INVALIDE_REFUS      = "Cette demande ne peut pas être refusée (statut actuel : %s). "
                + "Seules les demandes EN_ATTENTE sont refusables.";
        static final String REMBOURSEMENT_STATUT_INVALIDE_ANNULATION = "Annulation impossible : seules les demandes EN_ATTENTE "
                + "peuvent être annulées (statut actuel : %s).";
        static final String MOTIF_REFUS_OBLIGATOIRE                  = "Un motif de refus est obligatoire.";
        static final String MONTANT_AJUSTE_DEPASSE_PLAFOND           = "Le montant ajusté (%.2f TND) dépasse le plafond contractuel (%.2f TND).";

        // Compte épargne
        static final String SAVINGS_ACCOUNT_INTROUVABLE = "Aucun compte épargne trouvé pour votre profil.";
        static final String SAVINGS_ACCOUNT_INACTIF     = "Votre compte épargne n'est pas actif (statut : %s).";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. SOUMISSION DE DEMANDE (FARMER)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public RemboursementDTO soumettreDemandeRemboursement(String sinistreId, String userId) {

        // ── 1-a. Chargement & propriété ──────────────────────────────────────
        Sinistre sinistre = sinistreRepository.findById(sinistreId)
                .orElseThrow(() -> new RuntimeException(ErreurMetier.SINISTRE_INTROUVABLE));

        if (!sinistre.getUser().getId().equals(userId)) {
            throw new RuntimeException(ErreurMetier.SINISTRE_NON_AUTORISE);
        }

        verifierTypeSinistreCouvert(sinistre);

        // ── 1-b. Unicité — vérifiée tôt pour éviter du travail inutile ───────
        if (remboursementRepository.existsBySinistreIdAndStatutNot(
                sinistreId, StatutRemboursement.ANNULE)) {
            throw new RuntimeException(ErreurMetier.SINISTRE_DOUBLON);
        }

        // ── 1-c. Contrat actif ────────────────────────────────────────────────
        Insurance insurance = insuranceRepository
                .findTopByUserIdAndStatus(userId, InsuranceStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException(ErreurMetier.ASSURANCE_INTROUVABLE));

        // ── 1-d. Vérifications d'éligibilité (bloquantes) ────────────────────
        verifierPrimesAJour(insurance);         // FIX 3 : seuil = 6 mois
        verifierPeriodeCouverture(insurance, sinistre);
        verifierDelaiCarence(insurance, sinistre);
        verifierFrequenceRemboursements(userId);
        verifierCarencePostRemboursement(userId);          // FIX 5 : nouveau
        verifierQuotaRemboursement(sinistre);

        // ── 1-e. Anti-fraude : ratio montant assuré / primes ─────────────────
        // FIX 4 : la méthode peut maintenant lever une exception (blocage dur)
        String warningCoherence = verifierCoherenceMontantAssure(insurance);
        boolean dossierSignale  = (warningCoherence != null);

        // ── 1-f. Calculs financiers ───────────────────────────────────────────
        ResultatCalcul calcul = effectuerCalculs(sinistre, insurance);

        if (calcul.montantDommages().compareTo(calcul.franchise()) <= 0) {
            throw new RuntimeException(String.format(
                    ErreurMetier.DOMMAGES_SOUS_FRANCHISE,
                    calcul.montantDommages(), calcul.franchise()));
        }

        // FIX 1 — vérifier que le montant net (après déduction des primes restantes) est positif
        if (calcul.montantFinal().compareTo(BigDecimal.ZERO) <= 0
                && calcul.primesRestantes().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException(String.format(
                    ErreurMetier.MONTANT_NET_NUL, calcul.primesRestantes()));
        }

        // ── 1-g. Détermination du statut ──────────────────────────────────────
        String avertissement = genererAvertissement(calcul);
        StatutRemboursement statut;

        if (calcul.montantFinal().compareTo(BigDecimal.ZERO) <= 0) {
            statut       = StatutRemboursement.REFUSE;
            avertissement = "Aucune indemnisation possible après application de la franchise "
                    + "et déduction des primes restantes.";

        } else if (dossierSignale) {
            // Dossier signalé → forcé EN_ATTENTE même si montant ≤ seuil auto
            statut       = StatutRemboursement.EN_ATTENTE;
            avertissement = warningCoherence + "\n\n" + avertissement;
            log.warn("🚩 Remboursement forcé EN_ATTENTE — dossier signalé userId={}", userId);

        } else if (calcul.montantFinal().compareTo(SEUIL_AUTO_VERSEMENT) <= 0) {
            verserDansSavingsAccount(userId, calcul.montantFinal(), sinistreId);
            statut = StatutRemboursement.PAYE;

        } else {
            statut = StatutRemboursement.EN_ATTENTE;
        }

        // ── 1-h. Persistance ─────────────────────────────────────────────────
        Remboursement remboursement = remboursementRepository.save(
                buildRemboursement(insurance, sinistre, calcul, statut, avertissement));

        log.info("Remboursement [{}] créé — Statut: {} — Montant net: {} TND — Primes déduites: {} TND — Signalé: {}",
                remboursement.getId(), statut,
                calcul.montantFinal(), calcul.primesRestantes(), dossierSignale);

        return buildDTO(remboursement, insurance, sinistreId, avertissement);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. SIMULATION (DRY RUN)
    //    Inclut maintenant toutes les vérifications d'éligibilité pour que
    //    la simulation reflète exactement ce qui se passera à la soumission.
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public RemboursementDTO simulerRemboursement(String sinistreId, String userId) {

        Sinistre sinistre = sinistreRepository.findById(sinistreId)
                .orElseThrow(() -> new RuntimeException(ErreurMetier.SINISTRE_INTROUVABLE));

        if (!sinistre.getUser().getId().equals(userId)) {
            throw new RuntimeException(ErreurMetier.SINISTRE_NON_AUTORISE);
        }

        Insurance insurance = insuranceRepository
                .findTopByUserIdAndStatus(userId, InsuranceStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException(ErreurMetier.ASSURANCE_INTROUVABLE));

        // Simulation avec les mêmes vérifications bloquantes qu'à la soumission
        // → le farmer voit les erreurs AVANT de cliquer "Soumettre"
        verifierPrimesAJour(insurance);
        verifierPeriodeCouverture(insurance, sinistre);
        verifierDelaiCarence(insurance, sinistre);
        verifierFrequenceRemboursements(userId);
        verifierCarencePostRemboursement(userId);
        verifierQuotaRemboursement(sinistre);
        verifierCoherenceMontantAssure(insurance); // peut bloquer si ratio > seuil dur

        ResultatCalcul calcul = effectuerCalculs(sinistre, insurance);

        String avertissement = "[SIMULATION] " + genererAvertissement(calcul);
        if (calcul.primesRestantes().compareTo(BigDecimal.ZERO) > 0) {
            avertissement += String.format(
                    " | Primes restantes déduites : %.2f TND", calcul.primesRestantes());
        }

        return RemboursementDTO.builder()
                .sinistreId(sinistreId)
                .insuranceId(insurance.getId())
                .policyNumber(insurance.getPolicyNumber())
                .montantDommagesDeclares(calcul.montantDommages())
                .montantFranchise(calcul.franchise())
                .montantRemboursableAvantRegles(calcul.montantRemboursable())
                .montantFinalRembourse(calcul.montantFinal())
                .primesRestantesDues(calcul.primesRestantes())   // FIX 1 — désormais renseigné
                .moisPayes(getMoisPayes(insurance))
                .moisRestants(insurance.getRemainingPayments() != null ? insurance.getRemainingPayments() : 0)
                .primeParMois(insurance.getAmountPerPayment())
                .avertissement(avertissement)
                .simulation(true)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. ANNULATION PAR LE FARMER
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void annulerDemande(String remboursementId, String userId) {
        Remboursement r = remboursementRepository.findById(remboursementId)
                .orElseThrow(() -> new RuntimeException(ErreurMetier.REMBOURSEMENT_INTROUVABLE));

        if (!r.getInsurance().getUser().getId().equals(userId)) {
            throw new RuntimeException(ErreurMetier.REMBOURSEMENT_NON_AUTORISE);
        }

        if (r.getStatut() != StatutRemboursement.EN_ATTENTE) {
            throw new RuntimeException(String.format(
                    ErreurMetier.REMBOURSEMENT_STATUT_INVALIDE_ANNULATION, r.getStatut()));
        }

        r.setStatut(StatutRemboursement.ANNULE);
        remboursementRepository.save(r);
        log.info("Demande {} annulée par userId={}", remboursementId, userId);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. APPROBATION PAR ADMIN
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public Remboursement approuverRemboursement(String remboursementId, String adminId, BigDecimal montantAjuste) {
        Remboursement r = remboursementRepository.findById(remboursementId)
                .orElseThrow(() -> new RuntimeException(ErreurMetier.REMBOURSEMENT_INTROUVABLE));

        if (r.getStatut() != StatutRemboursement.EN_ATTENTE) {
            throw new RuntimeException(String.format(
                    ErreurMetier.REMBOURSEMENT_STATUT_INVALIDE_APPROBATION, r.getStatut()));
        }

        BigDecimal montantFinal = (montantAjuste != null && montantAjuste.compareTo(BigDecimal.ZERO) > 0)
                ? montantAjuste.setScale(2, RoundingMode.HALF_UP)
                : r.getMontantFinalRembourse();

        BigDecimal plafond = calculerPlafond(r.getInsurance());
        if (montantFinal.compareTo(plafond) > 0) {
            throw new RuntimeException(String.format(
                    ErreurMetier.MONTANT_AJUSTE_DEPASSE_PLAFOND, montantFinal, plafond));
        }

        verserDansSavingsAccount(r.getInsurance().getUser().getId(), montantFinal, r.getSinistre().getId());

        r.setMontantFinalRembourse(montantFinal);
        r.setStatut(StatutRemboursement.PAYE);
        r.setApprouveParAdminId(adminId);
        r.setDateRemboursement(LocalDate.now());

        log.info("Remboursement {} approuvé par admin {} — Montant versé : {} TND",
                remboursementId, adminId, montantFinal);

        return remboursementRepository.save(r);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5. REFUS PAR ADMIN
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public Remboursement refuserRemboursement(String remboursementId, String motif) {
        Remboursement r = remboursementRepository.findById(remboursementId)
                .orElseThrow(() -> new RuntimeException(ErreurMetier.REMBOURSEMENT_INTROUVABLE));

        if (r.getStatut() != StatutRemboursement.EN_ATTENTE) {
            throw new RuntimeException(String.format(
                    ErreurMetier.REMBOURSEMENT_STATUT_INVALIDE_REFUS, r.getStatut()));
        }

        if (motif == null || motif.isBlank()) {
            throw new RuntimeException(ErreurMetier.MOTIF_REFUS_OBLIGATOIRE);
        }

        r.setStatut(StatutRemboursement.REFUSE);
        r.setMotifRefus(motif);
        log.info("Remboursement {} refusé — motif : {}", remboursementId, motif);
        return remboursementRepository.save(r);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 6. LECTURE — FARMER
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<Remboursement> getRemboursementsByUser(String userId) {
        return remboursementRepository.findByInsuranceUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Remboursement getRemboursementById(String remboursementId, User user) {
        Remboursement r = remboursementRepository.findById(remboursementId)
                .orElseThrow(() -> new RuntimeException(ErreurMetier.REMBOURSEMENT_INTROUVABLE));

        if (user.getRole() == Role.FARMER && !r.getInsurance().getUser().getId().equals(user.getId())) {
            throw new RuntimeException(ErreurMetier.REMBOURSEMENT_NON_AUTORISE);
        }
        return r;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 7. LECTURE — ADMIN
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<Remboursement> getAllRemboursements(StatutRemboursement statut, Pageable pageable) {
        return statut != null
                ? remboursementRepository.findByStatut(statut, pageable)
                : remboursementRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Remboursement> getRemboursementsByStatut(StatutRemboursement statut) {
        return remboursementRepository.findByStatut(statut);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Remboursement> getRemboursementsByUserId(String userId) {
        return remboursementRepository.findByInsuranceUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Remboursement> getRemboursementsSuspects() {
        return remboursementRepository.findSuspects(
                LocalDate.now().minusYears(1), MAX_REMBOURSEMENTS_PAR_AN);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 8. STATISTIQUES ADMIN
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getRemboursementStats() {
        Map<String, Object> stats = new HashMap<>();

        long total     = remboursementRepository.count();
        long enAttente = remboursementRepository.countByStatut(StatutRemboursement.EN_ATTENTE);
        long payes     = remboursementRepository.countByStatut(StatutRemboursement.PAYE);
        long refuses   = remboursementRepository.countByStatut(StatutRemboursement.REFUSE);
        long annules   = remboursementRepository.countByStatut(StatutRemboursement.ANNULE);

        BigDecimal totalPaye = remboursementRepository.sumMontantFinalRembourseByStatut(StatutRemboursement.PAYE);

        stats.put("totalRemboursements",     total);
        stats.put("enAttente",               enAttente);
        stats.put("payes",                   payes);
        stats.put("refuses",                 refuses);
        stats.put("annules",                 annules);
        stats.put("totalMontantPaye",        totalPaye != null ? totalPaye : BigDecimal.ZERO);
        stats.put("tauxApprobation",         total == 0 ? 0 : Math.round((double) payes / total * 100));
        stats.put("seuilAutoVersement",      SEUIL_AUTO_VERSEMENT);
        stats.put("moisCarencePostRemb",     MOIS_CARENCE_POST_REMBOURSEMENT);
        stats.put("ratioAlerteAntiFraude",   RATIO_ALERTE_TOTAL_PRIMES);
        stats.put("ratioBloquantAntiFraude", RATIO_BLOQUANT_TOTAL_PRIMES);
        stats.put("typesCouverts",           TYPES_COUVERTS.stream().map(Enum::name).toList());

        return stats;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NOYAU DE CALCUL
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * FIX 1 — Le calcul intègre désormais la déduction des primes restantes dûes.
     *
     * Formule complète :
     *   montantDommages    = montantAssuré × quotaExpert(%)
     *   franchise          = montantDommages × tauxFranchise
     *   apresFranchise     = montantDommages − franchise
     *   montantRemboursable= apresFranchise × tauxCouverture
     *   plafond            = montantAssuré × tauxCouverture
     *   avantDeduction     = min(montantRemboursable, plafond)
     *   primesRestantes    = moisRestants × primeParMois
     *   montantFinal       = max(avantDeduction − primesRestantes, 0)
     */
    private ResultatCalcul effectuerCalculs(Sinistre sinistre, Insurance insurance) {
        BigDecimal montantDommages     = calculerMontantDommages(sinistre, insurance);
        BigDecimal franchise           = calculerFranchise(montantDommages, insurance);
        BigDecimal apresFranchise      = montantDommages.subtract(franchise).max(BigDecimal.ZERO);
        BigDecimal montantRemboursable = calculerMontantRemboursable(apresFranchise, insurance);
        BigDecimal plafond             = calculerPlafond(insurance);
        BigDecimal avantDeduction      = montantRemboursable.min(plafond).setScale(2, RoundingMode.HALF_UP);

        // FIX 1 — calcul des primes restantes non encore payées
        BigDecimal primesRestantes = calculerPrimesRestantes(insurance);

        // Le montant final ne peut pas être négatif
        BigDecimal montantFinal = avantDeduction.subtract(primesRestantes)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return new ResultatCalcul(
                montantDommages, franchise, apresFranchise,
                montantRemboursable, plafond, primesRestantes, montantFinal);
    }

    private record ResultatCalcul(
            BigDecimal montantDommages,
            BigDecimal franchise,
            BigDecimal apresFranchise,
            BigDecimal montantRemboursable,
            BigDecimal plafond,
            BigDecimal primesRestantes,   // FIX 1 — nouveau champ
            BigDecimal montantFinal
    ) {}

    private BigDecimal calculerMontantDommages(Sinistre sinistre, Insurance insurance) {
        float quota = sinistre.getQuotaRemboursement();
        return insurance.getInsuredAmount()
                .multiply(BigDecimal.valueOf(quota / 100.0))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculerFranchise(BigDecimal montantDommages, Insurance insurance) {
        // Aligné avec FORMULAS : BASIC=30%, STANDARD=20%, PREMIUM=10%
        BigDecimal taux = switch (insurance.getCoverageType()) {
            case BASIC    -> BigDecimal.valueOf(0.30);
            case STANDARD -> BigDecimal.valueOf(0.20);
            case PREMIUM  -> BigDecimal.valueOf(0.10);
        };
        return montantDommages.multiply(taux).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculerMontantRemboursable(BigDecimal apresFranchise, Insurance insurance) {
        BigDecimal taux = switch (insurance.getCoverageType()) {
            case BASIC    -> BigDecimal.valueOf(0.75);
            case STANDARD -> BigDecimal.valueOf(0.85);
            case PREMIUM  -> BigDecimal.valueOf(0.95);
        };
        return apresFranchise.multiply(taux).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculerPlafond(Insurance insurance) {
        BigDecimal taux = switch (insurance.getCoverageType()) {
            case BASIC    -> BigDecimal.valueOf(0.75);
            case STANDARD -> BigDecimal.valueOf(0.85);
            case PREMIUM  -> BigDecimal.valueOf(0.95);
        };
        return insurance.getInsuredAmount().multiply(taux).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * FIX 1 — Calcule le montant des primes restantes non encore payées.
     * Ce montant est déduit du remboursement : le farmer paie ce qu'il doit,
     * même s'il résilie après avoir touché l'indemnité.
     */
    private BigDecimal calculerPrimesRestantes(Insurance insurance) {
        int moisRestants = insurance.getRemainingPayments() != null
                ? insurance.getRemainingPayments() : 0;
        if (moisRestants <= 0) return BigDecimal.ZERO;

        BigDecimal primeParMois = insurance.getAmountPerPayment();
        if (primeParMois == null || primeParMois.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return primeParMois.multiply(BigDecimal.valueOf(moisRestants))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VÉRIFICATIONS D'ÉLIGIBILITÉ
    // ═══════════════════════════════════════════════════════════════════════════

    private void verifierTypeSinistreCouvert(Sinistre sinistre) {
        if (!TYPES_COUVERTS.contains(sinistre.getTypeSinistre())) {
            throw new RuntimeException(String.format(
                    ErreurMetier.SINISTRE_TYPE_NON_COUVERT, sinistre.getTypeSinistre()));
        }
    }

    private void verifierPrimesAJour(Insurance insurance) {
        if (insurance.isOverdue()) {
            throw new RuntimeException(ErreurMetier.PRIMES_EN_RETARD);
        }
    }


    private void verifierPeriodeCouverture(Insurance insurance, Sinistre sinistre) {
        if (insurance.getStartDate() == null || insurance.getEndDate() == null
                || sinistre.getDateCatastrophe() == null) {
            return;
        }
        LocalDate dateSinistre = sinistre.getDateCatastrophe().toLocalDate();
        if (dateSinistre.isBefore(insurance.getStartDate())
                || dateSinistre.isAfter(insurance.getEndDate())) {
            throw new RuntimeException(ErreurMetier.SINISTRE_HORS_PERIODE);
        }
    }

    private void verifierDelaiCarence(Insurance insurance, Sinistre sinistre) {
        if (insurance.getStartDate() == null || sinistre.getDateCatastrophe() == null) return;

        LocalDate finCarence    = insurance.getStartDate().plusDays(JOURS_CARENCE);
        LocalDate dateSinistre  = sinistre.getDateCatastrophe().toLocalDate();

        if (dateSinistre.isBefore(finCarence)) {
            throw new RuntimeException(String.format(
                    ErreurMetier.CARENCE_NON_ECOULEE, JOURS_CARENCE, dateSinistre));
        }
    }

    /**
     * FIX 5 — Carence post-remboursement.
     * Interdit une nouvelle demande dans les N mois suivant un versement PAYÉ.
     * Empêche les cycles rapides : sinistre → remboursement → nouveau sinistre immédiat.
     *
     * Requête à ajouter dans RemboursementRepository :
     *   Optional<Remboursement> findFirstByInsuranceUserIdAndStatutOrderByDateRemboursementDesc(
     *       String userId, StatutRemboursement statut);
     */
    private void verifierCarencePostRemboursement(String userId) {
        Optional<Remboursement> dernierPaye = remboursementRepository
                .findFirstByInsuranceUserIdAndStatutOrderByDateRemboursementDesc(
                        userId, StatutRemboursement.PAYE);

        if (dernierPaye.isEmpty()) return;

        LocalDate dateDernierRemb  = dernierPaye.get().getDateRemboursement();
        LocalDate dateProchainDroit = dateDernierRemb.plusMonths(MOIS_CARENCE_POST_REMBOURSEMENT);

        if (LocalDate.now().isBefore(dateProchainDroit)) {
            throw new RuntimeException(String.format(
                    ErreurMetier.CARENCE_POST_REMBOURSEMENT,
                    dateProchainDroit,
                    MOIS_CARENCE_POST_REMBOURSEMENT,
                    dateDernierRemb));
        }
    }

    /**
     * FIX 4 — Anti-fraude avec deux seuils distincts.
     *
     *  • Ratio > RATIO_BLOQUANT (25×) → exception immédiate, demande refusée.
     *  • Ratio > RATIO_ALERTE   (15×) → warning retourné, dossier forcé EN_ATTENTE.
     *  • Sinon                        → null (aucun problème détecté).
     *
     * @return warning message si alerte, null si aucun problème
     * @throws RuntimeException si le ratio dépasse le seuil bloquant
     */
    private String verifierCoherenceMontantAssure(Insurance insurance) {
        BigDecimal totalPrimes = insurance.getTotalPremium();
        if (totalPrimes == null || totalPrimes.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        double ratio       = insurance.getInsuredAmount().doubleValue() / totalPrimes.doubleValue();
        BigDecimal seuilBloquant = totalPrimes.multiply(BigDecimal.valueOf(RATIO_BLOQUANT_TOTAL_PRIMES));
        BigDecimal seuilAlerte   = totalPrimes.multiply(BigDecimal.valueOf(RATIO_ALERTE_TOTAL_PRIMES));

        // Seuil bloquant — refus automatique
        if (insurance.getInsuredAmount().compareTo(seuilBloquant) > 0) {
            log.error("🚫 Anti-fraude bloquant — ratio {:.1f}x pour userId={}",
                    ratio, insurance.getUser().getId());
            throw new RuntimeException(String.format(
                    ErreurMetier.MONTANT_ASSURE_BLOQUANT,
                    insurance.getInsuredAmount(),
                    totalPrimes,
                    ratio,
                    RATIO_BLOQUANT_TOTAL_PRIMES));
        }

        // Seuil alerte — non bloquant, validation admin requise
        if (insurance.getInsuredAmount().compareTo(seuilAlerte) > 0) {
            String warning = String.format(
                    "Dossier signalé : montant assuré (%.2f TND) élevé par rapport "
                            + "aux primes (%.2f TND). Ratio : %.1fx (seuil alerte : %dx). "
                            + "Validation admin obligatoire.",
                    insurance.getInsuredAmount(),
                    totalPrimes,
                    ratio,
                    RATIO_ALERTE_TOTAL_PRIMES);
            log.warn("🚩 {}", warning);
            return warning;
        }

        return null;
    }

    private void verifierFrequenceRemboursements(String userId) {
        long count = remboursementRepository.countByInsuranceUserIdAndStatutAndDateRemboursementAfter(
                userId, StatutRemboursement.PAYE, LocalDate.now().minusYears(1));

        if (count >= MAX_REMBOURSEMENTS_PAR_AN) {
            throw new RuntimeException(String.format(
                    ErreurMetier.FREQUENCE_MAX_ATTEINTE, count, MAX_REMBOURSEMENTS_PAR_AN));
        }
    }

    private void verifierQuotaRemboursement(Sinistre sinistre) {
        Float quota = sinistre.getQuotaRemboursement();
        if (quota == null || quota <= 0f) {
            throw new RuntimeException(ErreurMetier.SINISTRE_QUOTA_MANQUANT);
        }
        if (quota > 100f) {
            throw new RuntimeException(String.format(ErreurMetier.SINISTRE_QUOTA_INVALIDE, quota));
        }
    }

    private int getMoisPayes(Insurance insurance) {
        int total    = insurance.getNumberOfPayments()  != null ? insurance.getNumberOfPayments()  : 12;
        int restants = insurance.getRemainingPayments() != null ? insurance.getRemainingPayments() : 0;
        return Math.max(0, total - restants);
    }

    /**
     * FIX 1 — L'avertissement inclut maintenant la ligne de déduction des primes.
     */
    private String genererAvertissement(ResultatCalcul c) {
        String base = String.format(
                "Dommages : %.2f TND | Franchise : %.2f TND | Après franchise : %.2f TND | "
                        + "Remboursable : %.2f TND | Plafond : %.2f TND",
                c.montantDommages(), c.franchise(), c.apresFranchise(),
                c.montantRemboursable(), c.plafond());

        if (c.primesRestantes().compareTo(BigDecimal.ZERO) > 0) {
            base += String.format(" | Primes restantes déduites : %.2f TND", c.primesRestantes());
        }

        base += String.format(" | Montant final net : %.2f TND", c.montantFinal());
        return base;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDERS
    // ═══════════════════════════════════════════════════════════════════════════

    private Remboursement buildRemboursement(Insurance insurance, Sinistre sinistre,
                                             ResultatCalcul calcul, StatutRemboursement statut,
                                             String avertissement) {
        return Remboursement.builder()
                .insurance(insurance)
                .sinistre(sinistre)
                .montantDommagesDeclares(calcul.montantDommages())
                .montantFranchise(calcul.franchise())
                .montantRemboursableAvantRegles(calcul.montantRemboursable())
                .montantApresProrata(calcul.montantRemboursable().subtract(calcul.primesRestantes())
                        .max(BigDecimal.ZERO))
                .montantFinalRembourse(calcul.montantFinal())
                .avertissement(avertissement)
                .statut(statut)
                .dateRemboursement(statut == StatutRemboursement.PAYE ? LocalDate.now() : null)
                // ✅ AJOUT des 3 champs manquants :
                .primesRestantesDues(calcul.primesRestantes())
                .moisRestants(insurance.getRemainingPayments() != null ? insurance.getRemainingPayments() : 0)
                .primeParMois(insurance.getAmountPerPayment())
                .build();
    }

    private RemboursementDTO buildDTO(Remboursement r, Insurance insurance,
                                      String sinistreId, String avertissement) {
        return RemboursementDTO.builder()
                .remboursementId(r.getId())
                .sinistreId(sinistreId)
                .insuranceId(insurance.getId())
                .policyNumber(insurance.getPolicyNumber())
                .montantDommagesDeclares(r.getMontantDommagesDeclares())
                .montantFranchise(r.getMontantFranchise())
                .montantRemboursableAvantRegles(r.getMontantRemboursableAvantRegles())
                // FIX 1 — renseigne le champ déjà prévu dans le DTO
                .primesRestantesDues(calculerPrimesRestantes(insurance))
                .montantFinalRembourse(r.getMontantFinalRembourse())
                .moisPayes(getMoisPayes(insurance))
                .moisRestants(insurance.getRemainingPayments() != null ? insurance.getRemainingPayments() : 0)
                .primeParMois(insurance.getAmountPerPayment())
                .statut(r.getStatut())
                .dateRemboursement(r.getDateRemboursement())
                .approuveParAdminId(r.getApprouveParAdminId())
                .motifRefus(r.getMotifRefus())
                .avertissement(avertissement)
                .simulation(false)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VERSEMENT SUR COMPTE ÉPARGNE
    // ═══════════════════════════════════════════════════════════════════════════

    private void verserDansSavingsAccount(String userId, BigDecimal montant, String sinistreId) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException(ErreurMetier.SAVINGS_ACCOUNT_INTROUVABLE));

        if (account.getStatus() != SavingsAccountStatus.ACTIVE) {
            throw new RuntimeException(String.format(
                    ErreurMetier.SAVINGS_ACCOUNT_INACTIF, account.getStatus()));
        }

        BigDecimal nouveauSolde = account.getCurrentBalance().add(montant)
                .setScale(2, RoundingMode.HALF_UP);
        account.setCurrentBalance(nouveauSolde);

        savingsTransactionRepository.save(SavingsTransaction.builder()
                .account(account)
                .type(SavingsTransactionType.DEPOSIT)
                .amount(montant)
                .description("Indemnisation sinistre #" + sinistreId)
                .occurredAt(LocalDateTime.now())
                .build());

        savingsAccountRepository.save(account);

        log.info("Versement de {} TND effectué sur le compte épargne de userId={} pour sinistre={}",
                montant, userId, sinistreId);
    }
}