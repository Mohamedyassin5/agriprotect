package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.entities.IndemnisationRequest;
import tn.esprit.agri.entities.Investigation;
import tn.esprit.agri.entities.SolidarityFund;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.InvestigationStatus;
import tn.esprit.agri.entities.enums.InvestigationType;
import tn.esprit.agri.entities.enums.Role;
import tn.esprit.agri.repositories.IndemnisationRequestRepository;
import tn.esprit.agri.repositories.InvestigationRepository;
import tn.esprit.agri.repositories.SolidarityFundRepository;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.InvestigationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestigationServiceImpl implements InvestigationService {

    private final InvestigationRepository investigationRepository;
    private final IndemnisationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final SolidarityFundRepository fundRepository;

    @Override
    @Transactional
    public Investigation fileInvestigation(String farmerId, String requestId, InvestigationType type, String description) {
        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        if (farmer.getRole() != Role.FARMER) {
            throw new RuntimeException("Only a FARMER can file an investigation.");
        }

        IndemnisationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Indemnisation request not found"));

        if (!request.getFarmer().getId().equals(farmerId)) {
            throw new RuntimeException("This request does not belong to you.");
        }

        if (request.getStatus() != IndemnisationRequest.RequestStatus.REFUSED) {
            throw new RuntimeException("You can only open an investigation or reclamation for a REFUSED request.");
        }

        if (investigationRepository.existsByIndemnisationRequestId(requestId)) {
            throw new RuntimeException("An investigation already exists for this request.");
        }

        SolidarityFund fund = request.getFund();

        // Find an expert assigned to this fund
        User expert = userRepository.findFirstByRoleAndExpertFundId(Role.EXPERT, fund.getId())
                .orElseThrow(() -> new RuntimeException("No expert is currently assigned to this fund. Cannot proceed."));

        Investigation investigation = Investigation.builder()
                .farmer(farmer)
                .indemnisationRequest(request)
                .assignedExpert(expert)
                .type(type)
                .status(InvestigationStatus.PENDING)
                .description(description)
                .build();

        // Update the original request status back to PENDING while under review
        request.setStatus(IndemnisationRequest.RequestStatus.PENDING);
        requestRepository.save(request);

        return investigationRepository.save(investigation);
    }

    @Override
    public List<Investigation> getAssignedInvestigations(String expertId) {
        return investigationRepository.findByAssignedExpertIdOrderByCreatedAtDesc(expertId);
    }

    @Override
    @Transactional
    public Investigation decideOnInvestigation(String expertId, String investigationId, boolean accepted, String decisionReason) {
        Investigation investigation = investigationRepository.findById(investigationId)
                .orElseThrow(() -> new RuntimeException("Investigation not found"));

        if (!investigation.getAssignedExpert().getId().equals(expertId)) {
            throw new RuntimeException("You are not the assigned expert for this investigation.");
        }

        if (investigation.getStatus() != InvestigationStatus.PENDING) {
            throw new RuntimeException("This investigation has already been decided.");
        }

        IndemnisationRequest request = investigation.getIndemnisationRequest();
        SolidarityFund fund = request.getFund();
        User farmer = request.getFarmer();

        if (accepted) {
            Double amount = request.getRequestedAmount();

            if (fund.getCurrentBalance() < amount) {
                throw new RuntimeException("Fund balance insufficient to approve this request.");
            }

            // Transfer Funds
            fund.setCurrentBalance(fund.getCurrentBalance() - amount);
            Double currentBalance = farmer.getAccountBalance() != null ? farmer.getAccountBalance() : 0.0;
            farmer.setAccountBalance(currentBalance + amount);

            // Update statuses
            investigation.setStatus(InvestigationStatus.ACCEPTED);
            request.setStatus(IndemnisationRequest.RequestStatus.APPROVED);
            request.setProcessedDate(LocalDateTime.now());
            request.setProcessedBy(investigation.getAssignedExpert());

            fundRepository.save(fund);
            userRepository.save(farmer);

        } else {
            investigation.setStatus(InvestigationStatus.REFUSED);
            request.setStatus(IndemnisationRequest.RequestStatus.REFUSED);
            request.setRefusalReason("Refused by Expert: " + decisionReason);
            request.setProcessedDate(LocalDateTime.now());
            request.setProcessedBy(investigation.getAssignedExpert());
        }

        investigation.setDecisionReason(decisionReason);
        requestRepository.save(request);

        return investigationRepository.save(investigation);
    }
}
