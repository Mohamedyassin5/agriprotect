package tn.esprit.agri.services;

import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.Payment;

public interface PdfService {

    byte[] generateInvoice(Insurance insurance, Payment payment);
}