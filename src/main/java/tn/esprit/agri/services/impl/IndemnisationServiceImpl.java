package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.agri.DTO.AiAnalysisResponse;
import tn.esprit.agri.entities.*;
import tn.esprit.agri.repositories.*;
import tn.esprit.agri.services.AiVerificationService;
import tn.esprit.agri.services.IndemnisationService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IndemnisationServiceImpl implements IndemnisationService {

        private final IndemnisationRequestRepository requestRepository;
        private final UserRepository userRepository;
        private final SolidarityFundRepository fundRepository;
        private final FarmerSolidarityFundRepository farmerSolidarityFundRepository;
        private final AiVerificationService aiVerificationService;

        @Override
        @Transactional
        public IndemnisationRequest requestIndemnisation(
                        String farmerId,
                        String fundId,
                        Double amount,
                        String reason,
                        MultipartFile image) {

                User farmer = userRepository.findById(farmerId)
                                .orElseThrow(() -> new RuntimeException("Farmer not found"));

                SolidarityFund fund = fundRepository.findById(fundId)
                                .orElseThrow(() -> new RuntimeException("Fund not found"));

                // 1️⃣ Check active membership
                tn.esprit.agri.entities.FarmerSolidarityFundId membershipId = new tn.esprit.agri.entities.FarmerSolidarityFundId(
                                farmerId, fundId);

                tn.esprit.agri.entities.FarmerSolidarityFund membership = farmerSolidarityFundRepository
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

                // 3️⃣ AI Analysis (Before creating the request)
                AiAnalysisResponse aiResponse = aiVerificationService.verifyClaim(
                                reason,
                                fund.getCultureType(),
                                farmer.getAddress() != null ? farmer.getAddress() : "Unknown Location",
                                image);

                String requestId = farmer.getLastName() + "-" + fund.getId() + "-" + System.currentTimeMillis();

                // 4️⃣ Check fund balance (Auto-refusal rule)
                if (fund.getCurrentBalance() < amount) {
                        IndemnisationRequest request = IndemnisationRequest.builder()
                                        .id(requestId)
                                        .farmer(farmer)
                                        .fund(fund)
                                        .requestedAmount(amount)
                                        .requestDate(LocalDateTime.now())
                                        .status(IndemnisationRequest.RequestStatus.REFUSED)
                                        .requestReason(reason)
                                        .refusalReason("Automatic refusal: Insufficient fund balance")
                                        .processedDate(LocalDateTime.now())
                                        .aiScore(aiResponse.getConfidenceScore())
                                        .aiAnalysis(aiResponse.getAnalysisJustification())
                                        .build();
                        return requestRepository.save(request);
                }

                // 5️⃣ Create request with AI Result
                IndemnisationRequest.RequestStatus finalStatus = IndemnisationRequest.RequestStatus.PENDING;
                String refusalReason = null;

                // Auto-Decision Logic
                if (aiResponse.getConfidenceScore() >= 0.90) {
                        // High confidence - Auto approve if enough balance (already checked above)
                        finalStatus = IndemnisationRequest.RequestStatus.APPROVED;
                } else if (aiResponse.getConfidenceScore() <= 0.20) {
                        // Very low confidence - Auto refuse
                        finalStatus = IndemnisationRequest.RequestStatus.REFUSED;
                        refusalReason = "Automatic AI Refusal: " + aiResponse.getAnalysisJustification();
                }
                IndemnisationRequest request = IndemnisationRequest.builder()
                                .id(requestId)
                                .farmer(farmer)
                                .fund(fund)
                                .requestedAmount(amount)
                                .requestDate(LocalDateTime.now())
                                .status(finalStatus)
                                .requestReason(reason)
                                .aiScore(aiResponse.getConfidenceScore())
                                .aiAnalysis(aiResponse.getAnalysisJustification())
                                .refusalReason(refusalReason)
                                .build();

                if (finalStatus == IndemnisationRequest.RequestStatus.APPROVED) {
                        // Perform the fund transfer immediately if auto-approved
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
