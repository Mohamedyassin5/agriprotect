package tn.esprit.agri.services;

import tn.esprit.agri.entities.Insurance;

public interface PaymentReminderService {

    void checkAndSendReminders();           // À lancer tous les jours (via Scheduler)

}