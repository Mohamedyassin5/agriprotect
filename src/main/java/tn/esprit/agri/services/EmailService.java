package tn.esprit.agri.services;

import tn.esprit.agri.entities.Insurance;

public interface EmailService {
    void sendInsuranceConfirmationWithPdf(Insurance insurance);
    void sendContractToSignEmail(Insurance insurance);
    void sendActivationAndFirstPaymentEmail(Insurance insurance);
    void sendPaymentConfirmationEmail(Insurance insurance);
    void sendOverdueNotification(Insurance insurance);
    void sendPaymentReminder(Insurance insurance, String daysLeft);
    // Dans l'interface EmailService
    void sendCoverageSuspendedEmail(Insurance insurance);
    void sendRegularizationConfirmationEmail(Insurance insurance);
}
