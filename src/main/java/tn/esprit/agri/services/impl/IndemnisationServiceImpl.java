package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.DTO.AiAnalysisResponse;
import tn.esprit.agri.entities.*;
import tn.esprit.agri.repositories.*;
import tn.esprit.agri.services.AiVerificationService;
import tn.esprit.agri.services.IndemnisationService;
import tn.esprit.agri.services.VisionAiService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IndemnisationServiceImpl implements IndemnisationService {

        private final IndemnisationRequestRepository requestRepository;
        private final UserRepository userRepository;
        private final SolidarityFundRepository fundRepository;
        private final FarmerSolidarityFundRepository farmerSolidarityFundRepository;
        private final AiVerificationService aiVerificationService;
        private final SinistreRepository sinistreRepository;
        private final VisionAiService visionAiService;

        @Override
        @Transactional
        public IndemnisationRequest requestIndemnisation(
                String farmerId,
                String fundId,
                Double amount,
                String sinistreId,
                String farmerNotes,
                String damageType,
                Double affectedArea) {

                User farmer = userRepository.findById(farmerId)
                        .orElseThrow(() -> new RuntimeException("Farmer not found"));

                SolidarityFund fund = fundRepository.findById(fundId)
                        .orElseThrow(() -> new RuntimeException("Fund not found"));

                // Fetch the declared sinistre
                Sinistre sinistre = sinistreRepository.findById(sinistreId)
                        .orElseThrow(() -> new RuntimeException("Sinistre not found"));

                if (!sinistre.getUser().getId().equals(farmerId)) {
                        throw new RuntimeException("Ce sinistre ne vous appartient pas.");
                }

                // 1️⃣ Check active membership
                FarmerSolidarityFundId membershipId = new FarmerSolidarityFundId(farmerId, fundId);
                FarmerSolidarityFund membership = farmerSolidarityFundRepository
                        .findById(membershipId)
                        .orElseThrow(() -> new RuntimeException("No membership found"));

                if (membership.getStatus() != tn.esprit.agri.entities.enums.MembershipStatus.ACTIVE) {
                        throw new RuntimeException("Membership is not active");
                }

                // 2️⃣ Prevent multiple pending requests
                boolean hasPending = requestRepository
                        .existsByFarmerIdAndFundIdAndStatus(
                                farmerId,
                                fundId,
                                IndemnisationRequest.RequestStatus.PENDING);

                if (hasPending) {
                        throw new RuntimeException("You already have a pending request");
                }

                // Sanity check on amount vs area
                Double maxAllowedPerHectare = 15000.0;
                if (affectedArea != null && amount > (affectedArea * maxAllowedPerHectare)) {
                        throw new RuntimeException("Le montant demandé dépasse le plafond autorisé par hectare (" + maxAllowedPerHectare + " TND/ha).");
                }

                // 3️⃣ AI Analysis — combine sinistre description + farmer notes
                String sinistreDesc = sinistre.getDescription() != null ? sinistre.getDescription() : sinistre.getTypeSinistre().name();
                String reason = sinistreDesc
                        + (farmerNotes != null && !farmerNotes.isBlank()
                        ? ". Précisions agriculteur : " + farmerNotes : "")
                        + (damageType != null
                        ? ". Type de dommage : " + damageType : "")
                        + (affectedArea != null
                        ? ". Surface affectée : " + affectedArea + " ha" : "");
                AiAnalysisResponse aiResponse = aiVerificationService.verifyClaim(
                        reason,
                        fund.getCultureType(),
                        farmer.getAddress() != null ? farmer.getAddress() : "Unknown Location",
                        null);

                String requestId = farmer.getLastName() + "-" + fund.getId() + "-" + System.currentTimeMillis();

                // 4️⃣ Check fund balance (Auto-refusal rule)
                if (fund.getCurrentBalance() < amount) {
                        IndemnisationRequest request = IndemnisationRequest.builder()
                                .id(requestId)
                                .farmer(farmer)
                                .fund(fund)
                                .sinistre(sinistre)
                                .requestedAmount(amount)
                                .requestDate(LocalDateTime.now())
                                .status(IndemnisationRequest.RequestStatus.REFUSED)
                                .requestReason(reason)
                                .farmerNotes(farmerNotes)
                                .damageType(damageType != null ? IndemnisationRequest.DamageType.valueOf(damageType) : null)
                                .affectedArea(affectedArea)
                                .refusalReason("Automatic refusal: Insufficient fund balance")
                                .processedDate(LocalDateTime.now())
                                .aiScore(aiResponse.getConfidenceScore())
                                .aiAnalysis(aiResponse.getAnalysisJustification())
                                .imageProofUrl(sinistre.getImageUrl())
                                .build();
                        return requestRepository.save(request);
                }

                // 5️⃣ Create request with AI Result
                IndemnisationRequest.RequestStatus finalStatus = IndemnisationRequest.RequestStatus.PENDING;
                String refusalReason = null;

                if (aiResponse.getConfidenceScore() >= 0.90) {
                        finalStatus = IndemnisationRequest.RequestStatus.APPROVED;
                } else if (aiResponse.getConfidenceScore() <= 0.20) {
                        finalStatus = IndemnisationRequest.RequestStatus.REFUSED;
                        refusalReason = "Automatic AI Refusal: " + aiResponse.getAnalysisJustification();
                }

                IndemnisationRequest request = IndemnisationRequest.builder()
                        .id(requestId)
                        .farmer(farmer)
                        .fund(fund)
                        .sinistre(sinistre)
                        .requestedAmount(amount)
                        .requestDate(LocalDateTime.now())
                        .status(finalStatus)
                        .requestReason(reason)
                        .farmerNotes(farmerNotes)
                        .damageType(damageType != null ? IndemnisationRequest.DamageType.valueOf(damageType) : null)
                        .affectedArea(affectedArea)
                        .aiScore(aiResponse.getConfidenceScore())
                        .aiAnalysis(aiResponse.getAnalysisJustification())
                        .refusalReason(refusalReason)
                        .imageProofUrl(sinistre.getImageUrl())
                        .build();

                if (finalStatus == IndemnisationRequest.RequestStatus.APPROVED) {
                        fund.setCurrentBalance(fund.getCurrentBalance() - amount);
                        Double currentBalance = farmer.getAccountBalance() != null ? farmer.getAccountBalance() : 0.0;
                        farmer.setAccountBalance(currentBalance + amount);
                        request.setProcessedDate(LocalDateTime.now());
                        fundRepository.save(fund);
                        userRepository.save(farmer);
                }

                return requestRepository.save(request);
        }

        @Override
        public java.util.List<tn.esprit.agri.entities.FarmerSolidarityFund> getMemberships(String farmerId) {
                return farmerSolidarityFundRepository.findByFarmerId(farmerId);
        }

        @Override
        @Transactional
        public void processIndemnisation(String requestId, boolean approved, String refusalReason) {
                IndemnisationRequest request = requestRepository.findById(requestId)
                        .orElseThrow(() -> new RuntimeException("Request not found"));

                if (request.getStatus() != IndemnisationRequest.RequestStatus.PENDING) {
                        throw new RuntimeException("Request is not pending");
                }

                if (approved) {
                        SolidarityFund fund = request.getFund();
                        User farmer = request.getFarmer();
                        Double amount = request.getRequestedAmount();

                        if (fund.getCurrentBalance() < amount) {
                                throw new RuntimeException("Fund balance insufficient for payment");
                        }

                        // Transfer Funds
                        fund.setCurrentBalance(fund.getCurrentBalance() - amount);

                        // Handle null balance for existing users
                        Double currentBalance = farmer.getAccountBalance() != null ? farmer.getAccountBalance() : 0.0;
                        farmer.setAccountBalance(currentBalance + amount);

                        // Update Request
                        request.setStatus(IndemnisationRequest.RequestStatus.APPROVED);
                        request.setProcessedDate(LocalDateTime.now());

                        // Save Changes
                        fundRepository.save(fund);
                        userRepository.save(farmer);
                } else {
                        request.setStatus(IndemnisationRequest.RequestStatus.REFUSED);
                        request.setRefusalReason(refusalReason);
                        request.setProcessedDate(LocalDateTime.now());
                }

                requestRepository.save(request);
        }

        @Override
        public java.util.List<IndemnisationRequest> getAllPendingRequests() {
                return requestRepository.findAll().stream()
                        .filter(r -> r.getStatus() == IndemnisationRequest.RequestStatus.PENDING)
                        .toList();
        }

        @Override
        public java.util.List<IndemnisationRequest> getFarmerRequestHistory(String farmerId) {
                return requestRepository.findByFarmerId(farmerId);
        }
}
