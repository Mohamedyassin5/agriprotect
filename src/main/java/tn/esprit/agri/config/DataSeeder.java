package tn.esprit.agri.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.entities.AccountingEntry;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.EntryCategory;
import tn.esprit.agri.entities.enums.EntryType;
import tn.esprit.agri.entities.enums.Role;
import tn.esprit.agri.entities.enums.Status;
import tn.esprit.agri.repositories.AccountingEntryRepository;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.entities.Wallet;
import tn.esprit.agri.entities.WalletTransaction;
import tn.esprit.agri.entities.enums.WalletStatus;
import tn.esprit.agri.entities.enums.WalletTransactionType;
import tn.esprit.agri.repositories.WalletRepository;
import tn.esprit.agri.repositories.WalletTransactionRepository;
import tn.esprit.agri.entities.EmergencyWithdrawal;
import tn.esprit.agri.entities.enums.EmergencyWithdrawalReason;
import tn.esprit.agri.entities.enums.EmergencyWithdrawalStatus;
import tn.esprit.agri.repositories.EmergencyWithdrawalRepository;
import tn.esprit.agri.entities.Listing;
import tn.esprit.agri.entities.enums.ListingCategory;
import tn.esprit.agri.entities.enums.ListingStatus;
import tn.esprit.agri.entities.enums.ListingType;
import tn.esprit.agri.repositories.ListingRepository;
import tn.esprit.agri.entities.MarketplaceOrder;
import tn.esprit.agri.entities.enums.OrderStatus;
import tn.esprit.agri.entities.enums.PaymentSource;
import tn.esprit.agri.repositories.MarketplaceOrderRepository;
import tn.esprit.agri.entities.MarketplaceReview;
import tn.esprit.agri.repositories.MarketplaceReviewRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final EmergencyWithdrawalRepository emergencyWithdrawalRepository;
    private final ListingRepository listingRepository;
    private final MarketplaceOrderRepository orderRepository;
    private final MarketplaceReviewRepository reviewRepository;
    private final AccountingEntryRepository accountingEntryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {

        // Seed wallets pour TOUS les farmers existants qui n'en ont pas encore
        seedMissingWallets();

        // Si marketplace et wallets déjà seedés → stop
        if (listingRepository.count() > 0 && walletRepository.count() > 0) {
            log.info("[DataSeeder] Données déjà présentes — skip du seed complet.");
            return;
        }

        log.info("[DataSeeder] Insertion des données de démonstration...");

        // ── 1. USERS ──────────────────────────────────────────────────
        User admin = upsertUser("admin@agriprotect.tn",       "Admin", "AgriProtect", Role.ADMIN);
        User ali   = upsertUser("ali.ben.salah@agri.tn",      "Ali",   "Ben Salah",   Role.FARMER);
        User fatma = upsertUser("fatma.khalil@agri.tn",       "Fatma", "Khalil",      Role.FARMER);
        User omar  = upsertUser("omar.trabelsi@agri.tn",      "Omar",  "Trabelsi",    Role.FARMER);
        User leila = upsertUser("leila.mansouri@agri.tn",     "Leila", "Mansouri",    Role.FARMER);

        // ── 2. WALLETS ────────────────────────────────────────────────
        Wallet wAli   = seedWallet(ali,   bd("8450.00"),  bd("2200.00"), bd("5000.00"), bd("300.00"), true,  5);
        Wallet wFatma = seedWallet(fatma, bd("3120.50"),  bd("4800.00"), bd("6000.00"), bd("500.00"), true,  10);
        Wallet wOmar  = seedWallet(omar,  bd("620.00"),   bd("180.00"),  bd("3000.00"), bd("150.00"), false, 1);
        Wallet wLeila = seedWallet(leila, bd("15200.00"), bd("7500.00"), bd("8000.00"), bd("800.00"), true,  15);

        // ── 3. WALLET TRANSACTIONS ────────────────────────────────────
        seedTx(wAli,   WalletTransactionType.DEPOSIT,               bd("5000.00"), "Dépôt initial",               bd("0"),        bd("5000.00"));
        seedTx(wAli,   WalletTransactionType.MARKETPLACE_RECEIPT,   bd("1800.00"), "Vente semences tomate",       bd("5000.00"),  bd("6800.00"));
        seedTx(wAli,   WalletTransactionType.TRANSFER_TO_EMERGENCY, bd("2200.00"), "Constitution fonds urgence",  bd("6800.00"),  bd("4600.00"));
        seedTx(wAli,   WalletTransactionType.MARKETPLACE_PAYMENT,   bd("950.00"),  "Achat engrais NPK",           bd("4600.00"),  bd("3650.00"));
        seedTx(wAli,   WalletTransactionType.DEPOSIT,               bd("4800.00"), "Récolte blé vendue",          bd("3650.00"),  bd("8450.00"));

        seedTx(wFatma, WalletTransactionType.DEPOSIT,               bd("8000.00"), "Subvention agricole",         bd("0"),        bd("8000.00"));
        seedTx(wFatma, WalletTransactionType.TRANSFER_TO_EMERGENCY, bd("4800.00"), "Fonds urgence",               bd("8000.00"),  bd("3200.00"));
        seedTx(wFatma, WalletTransactionType.MARKETPLACE_PAYMENT,   bd("79.50"),   "Achat semences olivier",      bd("3200.00"),  bd("3120.50"));

        seedTx(wOmar,  WalletTransactionType.DEPOSIT,               bd("800.00"),  "Dépôt initial",               bd("0"),        bd("800.00"));
        seedTx(wOmar,  WalletTransactionType.WITHDRAWAL,            bd("180.00"),  "Retrait urgent",              bd("800.00"),   bd("620.00"));

        seedTx(wLeila, WalletTransactionType.DEPOSIT,               bd("10000.00"), "Capital départ",             bd("0"),        bd("10000.00"));
        seedTx(wLeila, WalletTransactionType.PEER_TRANSFER_IN,      bd("5200.00"),  "Virement reçu partenaire",   bd("10000.00"), bd("15200.00"));
        seedTx(wLeila, WalletTransactionType.TRANSFER_TO_EMERGENCY, bd("7500.00"),  "Fonds urgence max",          bd("15200.00"), bd("7700.00"));

        // ── 4. EMERGENCY WITHDRAWALS ──────────────────────────────────
        emergencyWithdrawalRepository.save(EmergencyWithdrawal.builder()
                .wallet(wOmar).amount(bd("150.00"))
                .reason(EmergencyWithdrawalReason.DROUGHT)
                .description("Sécheresse exceptionnelle – perte totale récolte orge. Besoin urgent pour acheter semences de remplacement.")
                .status(EmergencyWithdrawalStatus.PENDING_APPROVAL)
                .build());

        emergencyWithdrawalRepository.save(EmergencyWithdrawal.builder()
                .wallet(wFatma).amount(bd("320.00"))
                .reason(EmergencyWithdrawalReason.MEDICAL)
                .description("Hospitalisation d'urgence – frais médicaux non couverts par assurance.")
                .status(EmergencyWithdrawalStatus.APPROVED)
                .adminNote("Justificatifs vérifiés. Approuvé.")
                .approvedBy(admin.getId())
                .build());

        emergencyWithdrawalRepository.save(EmergencyWithdrawal.builder()
                .wallet(wAli).amount(bd("200.00"))
                .reason(EmergencyWithdrawalReason.EQUIPMENT_FAILURE)
                .description("Panne pompe à eau – remplacement immédiat requis.")
                .status(EmergencyWithdrawalStatus.AUTO_APPROVED)
                .adminNote("Auto-approuvé (< 20% du fonds)")
                .build());

        // ── 5. MARKETPLACE LISTINGS ───────────────────────────────────
        Listing l1 = listingRepository.save(Listing.builder()
                .seller(ali).title("Semences Tomate Cerise Bio – Lot 5kg")
                .description("Semences certifiées biologiques, variété cerise cocktail. Taux de germination > 95%. Idéal pour serre ou plein air. Production de la ferme Ben Salah, région Nabeul.")
                .category(ListingCategory.SEED).type(ListingType.SALE)
                .price(bd("85.00")).unit("kg").stock(50)
                .location("Nabeul").status(ListingStatus.ACTIVE)
                .images(List.of("https://images.unsplash.com/photo-1592921870789-04563d55041c?w=400"))
                .averageRating(4.7).reviewCount(3).build());

        Listing l2 = listingRepository.save(Listing.builder()
                .seller(fatma).title("Engrais Organique Compost Premium 25L")
                .description("Compost 100% naturel, maturé 6 mois. Riche en humus, azote, phosphore. Certifié agriculture biologique.")
                .category(ListingCategory.FERTILIZER).type(ListingType.SALE)
                .price(bd("45.00")).unit("sac").stock(200)
                .location("Sfax").status(ListingStatus.ACTIVE)
                .averageRating(4.5).reviewCount(5).build());

        Listing l3 = listingRepository.save(Listing.builder()
                .seller(leila).title("Tracteur John Deere 5075E – Location journée")
                .description("Tracteur 75ch en excellent état. Disponible avec ou sans chauffeur. Zone Bizerte et environs. Inclus charrue 3 disques.")
                .category(ListingCategory.EQUIPMENT).type(ListingType.RENT)
                .price(bd("350.00")).unit("jour").stock(1)
                .location("Bizerte").status(ListingStatus.ACTIVE)
                .averageRating(5.0).reviewCount(2).build());

        Listing l4 = listingRepository.save(Listing.builder()
                .seller(ali).title("Récolte Blé Dur 2025 – 1 tonne minimum")
                .description("Blé dur sélectionné, taux protéine 14%. Récolte juin 2025, stockage silo climatisé. Livraison possible région centre.")
                .category(ListingCategory.HARVEST).type(ListingType.SALE)
                .price(bd("920.00")).unit("tonne").stock(15)
                .location("Kairouan").status(ListingStatus.ACTIVE)
                .averageRating(4.8).reviewCount(4).build());

        Listing l5 = listingRepository.save(Listing.builder()
                .seller(fatma).title("Service Analyse Sol & Conseil Agronomique")
                .description("Analyse complète sol + rapport + plan de fertilisation personnalisé. Déplacement inclus 50km autour de Sfax. Résultat en 72h.")
                .category(ListingCategory.SERVICE).type(ListingType.SERVICE)
                .price(bd("180.00")).unit("parcelle").stock(10)
                .location("Sfax").status(ListingStatus.ACTIVE)
                .averageRating(4.9).reviewCount(6).build());

        Listing l6 = listingRepository.save(Listing.builder()
                .seller(leila).title("Huile d'Olive Extra Vierge – 5L Artisanale")
                .description("Première pression à froid, olives Chemlali récoltées manuellement. Acidité < 0.3%. Bidon hermétique.")
                .category(ListingCategory.HARVEST).type(ListingType.SALE)
                .price(bd("65.00")).unit("bidon 5L").stock(80)
                .location("Sousse").status(ListingStatus.ACTIVE)
                .averageRating(4.6).reviewCount(8).build());

        // Annonces EN ATTENTE
        Listing l7 = listingRepository.save(Listing.builder()
                .seller(omar).title("Semences Piment Fort Tunisien – Baklouti 500g")
                .description("Variété locale Baklouti certifiée, idéale pour production de harissa. ~3000 graines. Non traité.")
                .category(ListingCategory.SEED).type(ListingType.SALE)
                .price(bd("38.00")).unit("sachet").stock(100)
                .location("Nabeul").status(ListingStatus.PENDING).build());

        Listing l8 = listingRepository.save(Listing.builder()
                .seller(omar).title("Pesticide Bio Neem Oil 1L")
                .description("Huile de neem 100% naturelle, 3000ppm. Efficace contre puce blanche, aleurodes. Homologué agriculture biologique.")
                .category(ListingCategory.PESTICIDE).type(ListingType.SALE)
                .price(bd("28.50")).unit("litre").stock(150)
                .location("Tunis").status(ListingStatus.PENDING).build());

        Listing l9 = listingRepository.save(Listing.builder()
                .seller(ali).title("Location Système Irrigation Goutte-à-Goutte 1ha")
                .description("Kit complet 1 hectare: tuyaux PE 16mm, goutteurs 4L/h, filtre à sable, raccords. Installation incluse. Économie eau 60%.")
                .category(ListingCategory.EQUIPMENT).type(ListingType.RENT)
                .price(bd("1200.00")).unit("saison").stock(3)
                .location("Gabès").status(ListingStatus.PENDING).build());

        // ── 6. ORDERS ─────────────────────────────────────────────────
        MarketplaceOrder o1 = orderRepository.save(MarketplaceOrder.builder()
                .buyer(omar).listing(l1).quantity(2)
                .unitPrice(bd("85.00")).totalPrice(bd("170.00"))
                .status(OrderStatus.DELIVERED).paymentSource(PaymentSource.WALLET)
                .deliveryAddress("Route Mornag km 12, Nabeul").notes("Urgent avant saison").build());

        MarketplaceOrder o2 = orderRepository.save(MarketplaceOrder.builder()
                .buyer(leila).listing(l2).quantity(20)
                .unitPrice(bd("45.00")).totalPrice(bd("900.00"))
                .status(OrderStatus.CONFIRMED).paymentSource(PaymentSource.WALLET)
                .deliveryAddress("Ferme Mansouri, Route Akouda, Sousse").build());

        MarketplaceOrder o3 = orderRepository.save(MarketplaceOrder.builder()
                .buyer(ali).listing(l3).quantity(3)
                .unitPrice(bd("350.00")).totalPrice(bd("1050.00"))
                .status(OrderStatus.DELIVERED).paymentSource(PaymentSource.WALLET)
                .deliveryAddress("Ferme Ben Salah, Kairouan").notes("3 jours labour printemps").build());

        MarketplaceOrder o4 = orderRepository.save(MarketplaceOrder.builder()
                .buyer(fatma).listing(l4).quantity(2)
                .unitPrice(bd("920.00")).totalPrice(bd("1840.00"))
                .status(OrderStatus.SHIPPED).paymentSource(PaymentSource.WALLET)
                .deliveryAddress("Moulin Khalil, Route Monastir km 5, Sfax").build());

        MarketplaceOrder o5 = orderRepository.save(MarketplaceOrder.builder()
                .buyer(omar).listing(l6).quantity(4)
                .unitPrice(bd("65.00")).totalPrice(bd("260.00"))
                .status(OrderStatus.PENDING).paymentSource(PaymentSource.WALLET)
                .deliveryAddress("Trabelsi Omar, Zaghouan").build());

        MarketplaceOrder o6 = orderRepository.save(MarketplaceOrder.builder()
                .buyer(ali).listing(l5).quantity(1)
                .unitPrice(bd("180.00")).totalPrice(bd("180.00"))
                .status(OrderStatus.DELIVERED).paymentSource(PaymentSource.WALLET)
                .deliveryAddress("Parcelle Nord, Ferme Ben Salah").build());

        // ── 7. REVIEWS ────────────────────────────────────────────────
        reviewRepository.save(MarketplaceReview.builder()
                .author(omar).listing(l1).order(o1).rating(5)
                .comment("Excellentes semences, germination quasi parfaite! Je recommande vivement Ali.").build());

        reviewRepository.save(MarketplaceReview.builder()
                .author(ali).listing(l3).order(o3).rating(5)
                .comment("Tracteur impeccable, chauffeur professionnel. Labour terminé en 2.5 jours.").build());

        reviewRepository.save(MarketplaceReview.builder()
                .author(ali).listing(l5).order(o6).rating(5)
                .comment("Rapport très détaillé avec plan de fertilisation complet. Excellent travail!").build());

        // ── 8. ACCOUNTING ENTRIES ─────────────────────────────────────
        acc(ali,   EntryType.INCOME,  EntryCategory.SALES,      bd("4800.00"), "Vente récolte blé",         LocalDate.now().minusMonths(1));
        acc(ali,   EntryType.INCOME,  EntryCategory.SALES,      bd("1800.00"), "Vente semences tomate",     LocalDate.now().minusMonths(1));
        acc(ali,   EntryType.EXPENSE, EntryCategory.SEEDS,      bd("320.00"),  "Achat semences fourrage",   LocalDate.now().minusMonths(2));
        acc(ali,   EntryType.EXPENSE, EntryCategory.FERTILIZER, bd("950.00"),  "Engrais NPK 20-20-20",      LocalDate.now().minusMonths(2));
        acc(ali,   EntryType.EXPENSE, EntryCategory.LABOR,      bd("1200.00"), "Main d'oeuvre récolte",     LocalDate.now().minusMonths(1));
        acc(ali,   EntryType.INCOME,  EntryCategory.SALES,      bd("3200.00"), "Vente orge saison",         LocalDate.now().minusMonths(3));
        acc(ali,   EntryType.EXPENSE, EntryCategory.EQUIPMENT,  bd("480.00"),  "Entretien matériel",        LocalDate.now().minusMonths(3));
        acc(ali,   EntryType.EXPENSE, EntryCategory.IRRIGATION, bd("250.00"),  "Facture eau irrigation",    LocalDate.now().minusMonths(2));

        acc(fatma, EntryType.INCOME,  EntryCategory.SALES,      bd("6500.00"), "Vente huile olive",         LocalDate.now().minusMonths(1));
        acc(fatma, EntryType.INCOME,  EntryCategory.SALES,      bd("2200.00"), "Vente compost",             LocalDate.now().minusMonths(2));
        acc(fatma, EntryType.EXPENSE, EntryCategory.LABOR,      bd("1800.00"), "Cueillette olives",         LocalDate.now().minusMonths(2));
        acc(fatma, EntryType.EXPENSE, EntryCategory.EQUIPMENT,  bd("650.00"),  "Location presse à huile",   LocalDate.now().minusMonths(1));
        acc(fatma, EntryType.EXPENSE, EntryCategory.TRANSPORT,  bd("320.00"),  "Transport Sfax-Tunis",      LocalDate.now().minusMonths(1));
        acc(fatma, EntryType.INCOME,  EntryCategory.SALES,      bd("1840.00"), "Vente blé à Ali",           LocalDate.now().minusWeeks(3));

        acc(omar,  EntryType.INCOME,  EntryCategory.SALES,      bd("1200.00"), "Vente légumes marché",      LocalDate.now().minusMonths(1));
        acc(omar,  EntryType.EXPENSE, EntryCategory.SEEDS,      bd("180.00"),  "Semences piment",           LocalDate.now().minusMonths(2));
        acc(omar,  EntryType.EXPENSE, EntryCategory.FERTILIZER, bd("420.00"),  "Engrais urée",              LocalDate.now().minusMonths(2));
        acc(omar,  EntryType.EXPENSE, EntryCategory.IRRIGATION, bd("180.00"),  "Pompe eau réparation",      LocalDate.now().minusMonths(1));
        acc(omar,  EntryType.INCOME,  EntryCategory.SALES,      bd("850.00"),  "Vente piments secs",        LocalDate.now().minusMonths(3));

        acc(leila, EntryType.INCOME,  EntryCategory.SALES,      bd("9800.00"), "Vente huile olive premium", LocalDate.now().minusMonths(1));
        acc(leila, EntryType.INCOME,  EntryCategory.SALES,      bd("3500.00"), "Location tracteur 10j",     LocalDate.now().minusMonths(1));
        acc(leila, EntryType.INCOME,  EntryCategory.SALES,      bd("2100.00"), "Vente amandes",             LocalDate.now().minusMonths(2));
        acc(leila, EntryType.EXPENSE, EntryCategory.LABOR,      bd("2400.00"), "Saisonniers récolte",       LocalDate.now().minusMonths(2));
        acc(leila, EntryType.EXPENSE, EntryCategory.EQUIPMENT,  bd("1200.00"), "Entretien tracteur annuel", LocalDate.now().minusMonths(3));
        acc(leila, EntryType.EXPENSE, EntryCategory.INSURANCE,  bd("890.00"),  "Assurance récolte",         LocalDate.now().minusMonths(3));
        acc(leila, EntryType.EXPENSE, EntryCategory.FERTILIZER, bd("760.00"),  "Fertilisants bio oliveraie",LocalDate.now().minusMonths(2));

        log.info("[DataSeeder] ✅ {} annonces, {} commandes, {} wallets, {} transactions, {} entrées comptables",
                listingRepository.count(), orderRepository.count(),
                walletRepository.count(), walletTransactionRepository.count(),
                accountingEntryRepository.count());
    }

    // Seed un wallet réaliste pour chaque farmer qui n'en a pas encore
    private void seedMissingWallets() {
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.FARMER && !walletRepository.existsByUserId(u.getId()))
                .forEach(farmer -> {
                    Wallet w = walletRepository.save(Wallet.builder()
                            .user(farmer)
                            .availableBalance(bd("3200.00"))
                            .emergencyFundBalance(bd("850.00"))
                            .status(WalletStatus.ACTIVE)
                            .emergencyTargetAmount(bd("4000.00"))
                            .emergencyMonthlyContribution(bd("200.00"))
                            .emergencyAutoContribute(true)
                            .emergencyContributionDay(5)
                            .build());

                    walletTransactionRepository.save(WalletTransaction.builder()
                            .wallet(w).type(WalletTransactionType.DEPOSIT)
                            .amount(bd("4000.00")).description("Capital initial AgriProtect")
                            .balanceBefore(bd("0")).balanceAfter(bd("4000.00")).build());

                    walletTransactionRepository.save(WalletTransaction.builder()
                            .wallet(w).type(WalletTransactionType.TRANSFER_TO_EMERGENCY)
                            .amount(bd("850.00")).description("Constitution fonds d'urgence")
                            .balanceBefore(bd("4000.00")).balanceAfter(bd("3150.00")).build());

                    walletTransactionRepository.save(WalletTransaction.builder()
                            .wallet(w).type(WalletTransactionType.MARKETPLACE_RECEIPT)
                            .amount(bd("50.00")).description("Première vente marketplace")
                            .balanceBefore(bd("3150.00")).balanceAfter(bd("3200.00")).build());

                    // Seed demo accounting entries so the AI dashboard shows real metrics immediately
                    if (accountingEntryRepository.countByUserId(farmer.getId()) == 0) {
                        acc(farmer, EntryType.INCOME,  EntryCategory.SALES,      bd("2800.00"), "Vente récolte saison",       LocalDate.now().minusMonths(1));
                        acc(farmer, EntryType.INCOME,  EntryCategory.SALES,      bd("1500.00"), "Vente produits agricoles",   LocalDate.now().minusMonths(2));
                        acc(farmer, EntryType.INCOME,  EntryCategory.SALES,      bd("1200.00"), "Vente marché local",         LocalDate.now().minusMonths(3));
                        acc(farmer, EntryType.EXPENSE, EntryCategory.SEEDS,      bd("280.00"),  "Achat semences",             LocalDate.now().minusMonths(2));
                        acc(farmer, EntryType.EXPENSE, EntryCategory.FERTILIZER, bd("450.00"),  "Engrais et pesticides",      LocalDate.now().minusMonths(2));
                        acc(farmer, EntryType.EXPENSE, EntryCategory.LABOR,      bd("900.00"),  "Main d'œuvre saisonnière",   LocalDate.now().minusMonths(1));
                        acc(farmer, EntryType.EXPENSE, EntryCategory.EQUIPMENT,  bd("350.00"),  "Entretien matériel",         LocalDate.now().minusMonths(3));
                        acc(farmer, EntryType.EXPENSE, EntryCategory.TRANSPORT,  bd("180.00"),  "Transport produits",         LocalDate.now().minusMonths(1));
                    }

                    log.info("[DataSeeder] Wallet + données IA créés pour: {} (3200 TND dispo, 850 TND urgence)", farmer.getEmail());
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private BigDecimal bd(String val) { return new BigDecimal(val); }

    private User upsertUser(String email, String first, String last, Role role) {
        return userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("password123"))
                        .firstName(first).lastName(last)
                        .role(role).status(Status.ACTIVE).score(75.0f)
                        .build()));
    }

    private Wallet seedWallet(User user, BigDecimal available, BigDecimal emergency,
                              BigDecimal target, BigDecimal monthly, boolean auto, int day) {
        if (walletRepository.existsByUserId(user.getId()))
            return walletRepository.findByUserId(user.getId()).orElseThrow();
        return walletRepository.save(Wallet.builder()
                .user(user).availableBalance(available).emergencyFundBalance(emergency)
                .status(WalletStatus.ACTIVE).emergencyTargetAmount(target)
                .emergencyMonthlyContribution(monthly)
                .emergencyAutoContribute(auto).emergencyContributionDay(day)
                .build());
    }

    private void seedTx(Wallet wallet, WalletTransactionType type, BigDecimal amount,
                        String desc, BigDecimal before, BigDecimal after) {
        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet).type(type).amount(amount).description(desc)
                .balanceBefore(before).balanceAfter(after).build());
    }

    private void acc(User user, EntryType type, EntryCategory cat,
                     BigDecimal amount, String desc, LocalDate date) {
        accountingEntryRepository.save(AccountingEntry.builder()
                .user(user).entryType(type).category(cat)
                .amount(amount).description(desc).entryDate(date).build());
    }
}
