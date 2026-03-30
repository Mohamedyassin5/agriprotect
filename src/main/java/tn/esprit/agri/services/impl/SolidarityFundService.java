package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.dto.SolidarityFundResponse;
import tn.esprit.agri.entities.*;
import tn.esprit.agri.entities.enums.FundStatus;
import tn.esprit.agri.entities.enums.MembershipStatus;
import tn.esprit.agri.entities.enums.Role;
import tn.esprit.agri.exception.BusinessRuleException;
import tn.esprit.agri.repositories.*;
import tn.esprit.agri.services.SolidarityFundServiceInterface;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SolidarityFundService implements SolidarityFundServiceInterface {

    private final SolidarityFundRepository solidarityFundRepository;
    private final UserRepository userRepository;
    private final CropRepository cropRepository;
    private final FarmerSolidarityFundRepository farmerSolidarityFundRepository;

    @Override
    public SolidarityFund createFund(SolidarityFund fund, User admin) {

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Seul un admin peut créer un fonds");
        }

        // -------------------------------
        // 1️⃣ Uniqueness check
        // -------------------------------
        Optional<SolidarityFund> existingFund = solidarityFundRepository.findByNameAndCultureType(fund.getName(),
                fund.getCultureType());
        if (existingFund.isPresent()) {
            throw new BusinessRuleException(
                    "Un fonds avec ce nom et ce type de culture existe déjà.",
                    "DUPLICATE_FUND");
        }

        // -------------------------------
        // 2️⃣ Auto minScore = 40
        // -------------------------------
        fund.setMinScore(40);

        // -------------------------------
        // 3️⃣ Generate ID = name-cultureType
        // -------------------------------
        fund.setId(fund.getName() + "-" + fund.getCultureType());

        // -------------------------------
        // 4️⃣ Segmentation automatique
        // -------------------------------
        List<User> eligibleFarmers = userRepository.findByRole(Role.FARMER).stream()
                .filter(farmer -> farmer.getScore() >= fund.getMinScore())
                .filter(farmer -> cropRepository.findByUserId(farmer.getId()).stream()
                        .anyMatch(crop -> crop.getCropType().equalsIgnoreCase(fund.getCultureType())))
                .toList();

        if (eligibleFarmers.size() < 2) {
            throw new RuntimeException(
                    "Au moins 2 agriculteurs éligibles sont requis pour créer un fonds.");
        }

        // -------------------------------
        // 5️⃣ Fund setup
        // -------------------------------
        fund.setCreatedBy(admin);
        fund.setStatus(FundStatus.ACTIVE);
        fund.setCreationDate(LocalDateTime.now());
        fund.setCurrentBalance(0.0);

        SolidarityFund savedFund = solidarityFundRepository.save(fund);

        // -------------------------------
        // 6️⃣ Auto-add eligible farmers
        // -------------------------------
        for (User farmer : eligibleFarmers) {
            FarmerSolidarityFund membership = FarmerSolidarityFund.builder()
                    .id(new FarmerSolidarityFundId(farmer.getId(), savedFund.getId()))
                    .farmer(farmer)
                    .solidarityFund(savedFund)
                    .dateAdhesion(LocalDate.now())
                    .monthsPaid(0)
                    .totalPaid(0.0)
                    .status(MembershipStatus.ACTIVE)
                    .currentPrimeAmount(savedFund.getPrimeAmount())
                    .build();

            farmerSolidarityFundRepository.save(membership);
        }

        return savedFund;
    }

    @Override
    public List<SolidarityFund> getAllFunds() {
        return solidarityFundRepository.findAll();
    }

    @Override
    public Optional<SolidarityFund> getFundById(String id) {
        return solidarityFundRepository.findById(id);
    }

    @Override
    public void deleteFund(String id) {
        solidarityFundRepository.deleteById(id);
    }

    public SolidarityFundResponse mapToResponse(SolidarityFund fund) {
        return SolidarityFundResponse.builder()
                .id(fund.getId())
                .name(fund.getName())
                .numeroFond(fund.getNumeroFond())
                .createdByUsername(fund.getCreatedBy() != null ? fund.getCreatedBy().getUsername() : null)
                .build();
    }

    public SolidarityFund save(SolidarityFund fund) {
        return solidarityFundRepository.save(fund);
    }

    public void payPrime(String fundId, String farmerId, User user) {
        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isFarmer = user.getRole() == Role.FARMER;

        if (!isAdmin && !(isFarmer && user.getId().equals(farmerId))) {
            throw new BusinessRuleException(
                    "Seul un administrateur peut valider un paiement.",
                    "ADMIN_ONLY_REQUIRED");
        }

        SolidarityFund fund = solidarityFundRepository.findById(fundId)
                .orElseThrow(() -> new BusinessRuleException("Fonds non trouvé.", "FUND_NOT_FOUND"));

        FarmerSolidarityFund membership = farmerSolidarityFundRepository.findById(
                new FarmerSolidarityFundId(farmerId, fundId))
                .orElseThrow(() -> new BusinessRuleException(
                        "L'agriculteur n'est pas inscrit à ce fonds.", "MEMBERSHIP_NOT_FOUND"));

        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Impossible de payer : adhésion suspendue ou terminée.", "MEMBERSHIP_NOT_ACTIVE");
        }

        LocalDate today = LocalDate.now();
        if (membership.getLastPaymentDate() != null &&
                membership.getLastPaymentDate().getMonth() == today.getMonth() &&
                membership.getLastPaymentDate().getYear() == today.getYear()) {
            throw new BusinessRuleException(
                    "Un seul versement de cotisation par mois est autorisé.",
                    "MONTHLY_PAYMENT_LIMIT_REACHED");
        }

        // Apply discount if present
        Double discount = membership.getDiscountPercentage() != null ? membership.getDiscountPercentage() : 0.0;
        double baseAmount = membership.getCurrentPrimeAmount();
        double amountToPay = baseAmount * (1 - (discount / 100.0));

        membership.setMonthsPaid(membership.getMonthsPaid() + 1);
        membership.setTotalPaid(membership.getTotalPaid() + amountToPay);
        fund.setCurrentBalance(fund.getCurrentBalance() + amountToPay);
        membership.setLastPaymentDate(today);

        farmerSolidarityFundRepository.save(membership);
        solidarityFundRepository.save(fund);
    }
}
