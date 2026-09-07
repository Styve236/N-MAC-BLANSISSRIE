package com.pressing.pressing.api.sms;

import com.pressing.pressing.api.commande.Commande;
import com.pressing.pressing.api.client.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class NotificationSmsService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final SmsService smsService;
    private final SmsProperties smsProperties;

    public void envoyerConfirmationCommande(Commande commande) {
        Client client = commande.getClient();
        StringBuilder message = new StringBuilder();
        message.append("Bonjour ").append(client.getNom())
                .append(", votre commande ").append(commande.getNumeroTicket())
                .append(" d'un montant de ").append(formater(commande.getMontantTotal()))
                .append(" FCFA a bien été enregistrée.");
        if (commande.getDateRecuperationPrevue() != null) {
            message.append(" Retrait prévu le ")
                    .append(commande.getDateRecuperationPrevue().format(FORMAT_DATE)).append(".");
        }
        message.append(" Merci pour votre confiance. ").append(smsProperties.getNomPressing());
        message.append(" Votre reçu : ").append(lienRecu(commande.getNumeroTicket())).append(".");

        smsService.envoyer(TypeNotificationSms.COMMANDE_ENREGISTREE, client.getTelephone(), message.toString());
    }

    public void envoyerPaiementRecu(Commande commande, BigDecimal montant) {
        Client client = commande.getClient();
        BigDecimal reste = commande.getMontantTotal().subtract(commande.getMontantPaye());

        StringBuilder message = new StringBuilder();
        message.append("Bonjour ").append(client.getNom())
                .append(", paiement de ").append(formater(montant))
                .append(" FCFA bien reçu pour votre commande ").append(commande.getNumeroTicket())
                .append(". ");
        if (reste.compareTo(BigDecimal.ZERO) > 0) {
            message.append("Reste à payer : ").append(formater(reste)).append(" FCFA.");
        } else {
            message.append("Commande entièrement payée, merci.");
        }
        message.append(" ").append(smsProperties.getNomPressing());

        smsService.envoyer(TypeNotificationSms.PAIEMENT_RECU, client.getTelephone(), message.toString());
    }

    public void envoyerLingePret(Commande commande) {
        Client client = commande.getClient();
        String message = "Bonjour " + client.getNom()
                + ", votre linge (commande " + commande.getNumeroTicket()
                + ") est lavé et prêt. Vous pouvez venir le récupérer dès maintenant. "
                + smsProperties.getNomPressing();

        smsService.envoyer(TypeNotificationSms.LINGE_PRET, client.getTelephone(), message);
    }

    public void envoyerRappelImpaye(Commande commande) {
        Client client = commande.getClient();
        BigDecimal reste = commande.getMontantTotal().subtract(commande.getMontantPaye());

        String message = "Bonjour " + client.getNom()
                + ", votre commande " + commande.getNumeroTicket()
                + " a un reste à payer de " + formater(reste)
                + " FCFA. Merci de régulariser votre situation. "
                + smsProperties.getNomPressing();

        smsService.envoyer(TypeNotificationSms.RAPPEL_IMPAYE, client.getTelephone(), message);
    }

    public void envoyerRecuCommande(Commande commande) {
        Client client = commande.getClient();
        String message = "Bonjour " + client.getNom()
                + ", le reçu de votre commande " + commande.getNumeroTicket()
                + " est disponible ici : " + lienRecu(commande.getNumeroTicket())
                + ". " + smsProperties.getNomPressing();

        smsService.envoyer(TypeNotificationSms.RECU_COMMANDE, client.getTelephone(), message);
    }

    public void envoyerLivreCommande(Commande commande) {
        Client client = commande.getClient();
        StringBuilder message = new StringBuilder();
        message.append("Bonjour ").append(client.getNom())
                .append(", votre commande ").append(commande.getNumeroTicket())
                .append(" sera livrée à l'adresse : ").append(commande.getAdresseLivraison());
        if (commande.getDateLivraisonPrevue() != null) {
            message.append(" le ").append(commande.getDateLivraisonPrevue().format(FORMAT_DATE));
        }
        message.append(". ").append(smsProperties.getNomPressing());

        smsService.envoyer(TypeNotificationSms.LIVRAISON_PLANIFIEE, client.getTelephone(), message.toString());
    }

    private String lienRecu(String numeroTicket) {
        return smsProperties.getRecuBaseUrl() + "/recu/" + numeroTicket;
    }

    private String formater(BigDecimal montant) {
        return montant.stripTrailingZeros().toPlainString();
    }
}