package com.pressing.pressing.api.recu;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import com.pressing.pressing.api.commande.Commande;
import com.pressing.pressing.api.commande.CommandeRepository;
import com.pressing.pressing.api.commande.LigneCommande;
import com.pressing.pressing.api.commande.Tarif;
import com.pressing.pressing.api.sms.NotificationSmsService;
import com.pressing.pressing.api.sms.SmsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class RecuService {

    private final CommandeRepository commandeRepository;
    private final NotificationSmsService notificationSmsService;
    private final SmsProperties smsProperties;

    @Transactional(readOnly = true)
    public RecuDTO getRecuParCommandeId(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable avec l'id : " + commandeId));
        return toDTO(commande);
    }

    @Transactional(readOnly = true)
    public RecuDTO getRecuParNumeroTicket(String numeroTicket) {
        Commande commande = commandeRepository.findByNumeroTicket(numeroTicket)
                .orElseThrow(() -> new RessourceNotFoundException("Aucune commande pour le ticket : " + numeroTicket));
        return toDTO(commande);
    }

    public RecuDTO envoyerRecuParSms(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable avec l'id : " + commandeId));
        notificationSmsService.envoyerRecuCommande(commande);
        return toDTO(commande);
    }

    String genererLien(Commande commande) {
        return smsProperties.getRecuBaseUrl() + "/recu/" + commande.getNumeroTicket();
    }

    @Transactional(readOnly = true)
    public byte[] getRecuPdf(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable avec l'id : " + commandeId));
        return genererPdf(commande);
    }

    private byte[] genererPdf(Commande commande) {
        DateTimeFormatter dt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        Document doc = new Document(PageSize.A4, 30, 30, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font bold14 = new Font(Font.HELVETICA, 14, Font.BOLD);
            Font bold12 = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font norm10 = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font bold10 = new Font(Font.HELVETICA, 10, Font.BOLD);

            Paragraph titre = new Paragraph("PRESSING CHERIF", bold14);
            titre.setAlignment(Element.ALIGN_CENTER);
            doc.add(titre);
            Paragraph st = new Paragraph("REÇU DE COMMANDE", bold12);
            st.setAlignment(Element.ALIGN_CENTER);
            st.setSpacingAfter(10);
            doc.add(st);

            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.addCell(new PdfPCell(new Phrase("Numéro de ticket", bold10)) {});
            info.addCell(new PdfPCell(new Phrase(commande.getNumeroTicket(), bold10)) {});
            info.addCell(new PdfPCell(new Phrase("Client", norm10)) {});
            info.addCell(new PdfPCell(new Phrase(commande.getClient().getNom() + " · " + commande.getClient().getTelephone(), norm10)) {});
            info.addCell(new PdfPCell(new Phrase("Date création", norm10)) {});
            info.addCell(new PdfPCell(new Phrase(commande.getDateCreation() != null ? commande.getDateCreation().format(dt) : "-", norm10)) {});
            info.addCell(new PdfPCell(new Phrase("Retrait prévu", norm10)) {});
            info.addCell(new PdfPCell(new Phrase(commande.getDateRecuperationPrevue() != null ? commande.getDateRecuperationPrevue().format(dt) : "-", norm10)) {});
            info.addCell(new PdfPCell(new Phrase("Statut", norm10)) {});
            info.addCell(new PdfPCell(new Phrase(commande.getStatut() != null ? commande.getStatut().name() : "-", norm10)) {});
            doc.add(info);
            doc.add(new Paragraph(" "));

            Paragraph lt = new Paragraph("DÉTAIL DES PRESTATIONS", bold12);
            lt.setSpacingAfter(6);
            doc.add(lt);

            PdfPTable lignes = new PdfPTable(4);
            lignes.setWidthPercentage(100);
            String[] headers = {"Désignation", "Unité", "Quantité/Poids", "Montant (FCFA)"};
            for (String h : headers) {
                PdfPCell c = new PdfPCell(new Phrase(h, bold10));
                c.setBackgroundColor(new java.awt.Color(230, 230, 230));
                lignes.addCell(c);
            }
            for (LigneCommande l : commande.getLignes()) {
                String designation = l.getTarif() != null
                        ? (l.getTarif().getNom() != null ? l.getTarif().getNom() : l.getTarif().getTypevetement())
                        : (l.getDescription() != null ? l.getDescription() : "-");
                boolean kilo = l.getPoids() != null && l.getPoids().compareTo(BigDecimal.ZERO) > 0;
                lignes.addCell(new PdfPCell(new Phrase(designation, norm10)) {});
                lignes.addCell(new PdfPCell(new Phrase(kilo ? "Poids (kg)" : "Pièce", norm10)) {});
                lignes.addCell(new PdfPCell(new Phrase(kilo ? String.valueOf(l.getPoids()) : String.valueOf(l.getQuantite()), norm10)) {});
                lignes.addCell(new PdfPCell(new Phrase(formatArgent(l.getMontant()), norm10)) {});
            }
            doc.add(lignes);
            doc.add(new Paragraph(" "));

            BigDecimal remise = commande.getRemise() != null ? commande.getRemise() : BigDecimal.ZERO;
            BigDecimal paye = commande.getMontantPaye() != null ? commande.getMontantPaye() : BigDecimal.ZERO;
            BigDecimal total = commande.getMontantTotal() != null ? commande.getMontantTotal() : BigDecimal.ZERO;
            BigDecimal reste = total.subtract(remise).subtract(paye);

            PdfPTable totaux = new PdfPTable(2);
            totaux.setWidthPercentage(60);
            totaux.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totaux.addCell(new PdfPCell(new Phrase("Total", bold10)) {});
            totaux.addCell(new PdfPCell(new Phrase(formatArgent(total), bold10)) {});
            if (remise.compareTo(BigDecimal.ZERO) > 0) {
                totaux.addCell(new PdfPCell(new Phrase("Remise", norm10)) {});
                totaux.addCell(new PdfPCell(new Phrase("- " + formatArgent(remise), norm10)) {});
            }
            totaux.addCell(new PdfPCell(new Phrase("Payé", norm10)) {});
            totaux.addCell(new PdfPCell(new Phrase(formatArgent(paye), norm10)) {});
            totaux.addCell(new PdfPCell(new Phrase("Reste à payer", bold10)) {});
            totaux.addCell(new PdfPCell(new Phrase(formatArgent(reste), bold10)) {});
            doc.add(totaux);

            doc.add(new Paragraph(" "));
            Paragraph pdfooter = new Paragraph("Merci de votre confiance. Pressez l'écran pour plus d'informations. Imprimé le "
                    + java.time.LocalDateTime.now().format(dt), new Font(Font.HELVETICA, 8, Font.NORMAL, java.awt.Color.GRAY));
            pdfooter.setAlignment(Element.ALIGN_CENTER);
            doc.add(pdfooter);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF du reçu", e);
        }
        return out.toByteArray();
    }

    private String formatArgent(BigDecimal montant) {
        if (montant == null) return "0";
        return montant.setScale(0, java.math.RoundingMode.HALF_UP).toString();
    }

    private RecuDTO toDTO(Commande commande) {
        RecuDTO dto = new RecuDTO();
        dto.setIdcommande(commande.getIdcommande());
        dto.setNumeroTicket(commande.getNumeroTicket());
        dto.setLien(genererLien(commande));
        dto.setStatut(commande.getStatut() != null ? commande.getStatut().name() : null);
        dto.setNomClient(commande.getClient().getNom());
        dto.setTelephoneClient(commande.getClient().getTelephone());
        dto.setDateCreation(commande.getDateCreation());
        dto.setDateRecuperationPrevue(commande.getDateRecuperationPrevue());
        dto.setPoidsTotal(commande.getPoidsTotal());
        dto.setMontantTotal(commande.getMontantTotal());
        dto.setRemise(commande.getRemise());
        dto.setMontantPaye(commande.getMontantPaye());
        BigDecimal remise = commande.getRemise() != null ? commande.getRemise() : BigDecimal.ZERO;
        dto.setResteAPayer(commande.getMontantTotal().subtract(remise).subtract(commande.getMontantPaye()));
        dto.setLignes(commande.getLignes().stream().map(this::ligneToDTO).toList());
        dto.setPaiements(commande.getPaiements().stream().map(p -> {
            PaiementRecuDTO paiement = new PaiementRecuDTO();
            paiement.setMontant(p.getMontant());
            paiement.setMoyenPaiement(p.getMoyenPaiement() != null ? p.getMoyenPaiement().name() : null);
            paiement.setTypePaiement(p.getTypePaiement() != null ? p.getTypePaiement().name() : null);
            paiement.setDatePaiement(p.getDatePaiement());
            paiement.setReferenceTransaction(p.getReferenceTransaction());
            return paiement;
        }).toList());
        return dto;
    }

    private LigneRecuDTO ligneToDTO(LigneCommande ligne) {
        LigneRecuDTO dto = new LigneRecuDTO();
        Tarif tarif = ligne.getTarif();
        if (tarif != null) {
            dto.setDesignation(tarif.getNom() != null ? tarif.getNom() : tarif.getTypevetement());
            dto.setTypeNettoyage(tarif.getTypeNettoyage() != null ? tarif.getTypeNettoyage().name() : null);
        }
        if (ligne.getPoids() != null && ligne.getPoids().compareTo(BigDecimal.ZERO) > 0) {
            dto.setUnite("KILO");
        } else {
            dto.setUnite("PIECE");
        }
        dto.setQuantite(ligne.getQuantite());
        dto.setPoids(ligne.getPoids());
        dto.setMontant(ligne.getMontant());
        return dto;
    }
}