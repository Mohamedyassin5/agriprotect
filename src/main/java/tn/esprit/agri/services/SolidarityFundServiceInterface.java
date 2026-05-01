package tn.esprit.agri.services;

import tn.esprit.agri.entities.SolidarityFund;
import tn.esprit.agri.entities.User;

import java.util.List;
import java.util.Optional;

public interface SolidarityFundServiceInterface {

    SolidarityFund createFund(SolidarityFund fund, User admin);

    List<SolidarityFund> getAllFunds();

    Optional<SolidarityFund> getFundById(String id);

    void deleteFund(String id);

    void joinFund(String fundId, User farmer);

    void autoEnrollInMatchingFunds(User user, String cropType);
}