package com.pressing.pressing.api.export;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.pressing.pressing.api.Paiement.Paiement;
import com.pressing.pressing.api.Paiement.PaiementRepository;
import com.pressing.pressing.api.caisse.*;
import com.pressing.pressing.api.client.Client;
import com.pressing.pressing.api.client.ClientRepository;
import com.pressing.pressing.api.commande.*;
import com.pressing.pressing.api.fidelite.HistoriquePoint;
import com.pressing.pressing.api.fidelite.HistoriquePointRepository;
import com.pressing.pressing.api.inventaire.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final CommandeRepository commandeRepository;
    private final CommandeService commandeService;
    private final PaiementRepository paiementRepository;
    private final ClotureCaisseRepository clotureCaisseRepository;
    private final ClotureService clotureService;
    private final InventaireRepository inventaireRepository;
    private final VetementsRepository vetementsRepository;
    private final HistoriquePointRepository historiquePointRepository;
    private final ClientRepository clientRepository;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==================== COMMANDES ====================

    @Transactional(readOnly = true)
    public byte[] commandesPdf(LocalDate debut, LocalDate fin) {
        List<Commande> commandes = commandeService.lister(null, null, debut, fin);
        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(titre("Export commandes" + dateRange(debut, fin)));
            PdfPTable t = new PdfPTable(9);
            t.setWidths(new float[]{12, 22, 22, 14, 14, 14, 14, 14, 14});
            t.setWidthPercentage(100);
            headerRow(t, "Ticket", "Client", "Date", "Statut", "Total", "Payé", "Remise", "Reste", "Livraison");
            for (Commande c : commandes) {
                BigDecimal remise = c.getRemise() != null ? c.getRemise() : BigDecimal.ZERO;
                BigDecimal paye = c.getMontantPaye() != null ? c.getMontantPaye() : BigDecimal.ZERO;
                BigDecimal total = c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO;
                BigDecimal reste = total.subtract(remise).subtract(paye);
                t.addCell(cell(c.getNumeroTicket()));
                t.addCell(cell(c.getClient().getNom()));
                t.addCell(cell(c.getDateCreation() != null ? c.getDateCreation().format(DT) : ""));
                t.addCell(cell(c.getStatut() != null ? c.getStatut().name() : ""));
                t.addCell(cell(fmt(total)));
                t.addCell(cell(fmt(paye)));
                t.addCell(cell(fmt(remise)));
                t.addCell(cell(fmt(reste)));
                t.addCell(cell(c.getStatutLivraison() != null ? c.getStatutLivraison().name() : "-"));
            }
            doc.add(t);
            footer(doc);
            doc.close();
        } catch (Exception e) { throw new RuntimeException("Erreur génération PDF commandes", e); }
        return out.toByteArray();
    }

    @Transactional(readOnly = true)
    public String commandesCsv(LocalDate debut, LocalDate fin) {
        List<Commande> commandes = commandeService.lister(null, null, debut, fin);
        StringBuilder sb = new StringBuilder();
        sb.append("Ticket;Client;Date;Statut;Total;Paye;Remise;Reste;Livraison\n");
        for (Commande c : commandes) {
            BigDecimal remise = c.getRemise() != null ? c.getRemise() : BigDecimal.ZERO;
            BigDecimal paye = c.getMontantPaye() != null ? c.getMontantPaye() : BigDecimal.ZERO;
            BigDecimal total = c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO;
            BigDecimal reste = total.subtract(remise).subtract(paye);
            sb.append(c.getNumeroTicket()).append(";")
              .append(c.getClient().getNom()).append(";")
              .append(c.getDateCreation() != null ? c.getDateCreation().format(DT) : "").append(";")
              .append(c.getStatut()).append(";")
              .append(fmt(total)).append(";")
              .append(fmt(paye)).append(";")
              .append(fmt(remise)).append(";")
              .append(fmt(reste)).append(";")
              .append(c.getStatutLivraison()).append("\n");
        }
        return sb.toString();
    }

    // ==================== PAIEMENTS ====================

    @Transactional(readOnly = true)
    public byte[] paiementsPdf(LocalDate debut, LocalDate fin) {
        List<Paiement> paiements = paiementRepository.findByDatePaiementBetween(debut.atStartOfDay(), fin.atTime(23, 59, 59));
        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(titre("Export paiements" + dateRange(debut, fin)));
            PdfPTable t = new PdfPTable(6);
            t.setWidthPercentage(100);
            headerRow(t, "Date", "Commande", "Montant", "Moyen", "Type", "Réf. transaction");
            BigDecimal totalGeneral = BigDecimal.ZERO;
            for (Paiement p : paiements) {
                totalGeneral = totalGeneral.add(p.getMontant() != null ? p.getMontant() : BigDecimal.ZERO);
                t.addCell(cell(p.getDatePaiement() != null ? p.getDatePaiement().format(DT) : ""));
                t.addCell(cell(p.getCommande() != null ? p.getCommande().getNumeroTicket() : "-"));
                t.addCell(cell(fmt(p.getMontant())));
                t.addCell(cell(p.getMoyenPaiement() != null ? p.getMoyenPaiement().name() : ""));
                t.addCell(cell(p.getTypePaiement() != null ? p.getTypePaiement().name() : ""));
                t.addCell(cell(p.getReferenceTransaction() != null ? p.getReferenceTransaction() : ""));
            }
            PdfPCell totalCell = cell("TOTAL : " + fmt(totalGeneral) + " FCFA");
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalCell.setColspan(6);
            totalCell.setBackgroundColor(new java.awt.Color(230, 230, 230));
            totalCell.setPhrase(new Phrase("TOTAL : " + fmt(totalGeneral) + " FCFA", boldFont(10)));
            t.addCell(totalCell);
            doc.add(t);
            footer(doc);
            doc.close();
        } catch (Exception e) { throw new RuntimeException("Erreur génération PDF paiements", e); }
        return out.toByteArray();
    }

    @Transactional(readOnly = true)
    public String paiementsCsv(LocalDate debut, LocalDate fin) {
        List<Paiement> paiements = paiementRepository.findByDatePaiementBetween(debut.atStartOfDay(), fin.atTime(23, 59, 59));
        StringBuilder sb = new StringBuilder();
        sb.append("Date;Commande;Montant;Moyen;Type;Ref\n");
        for (Paiement p : paiements) {
            sb.append(p.getDatePaiement() != null ? p.getDatePaiement().format(DT) : "").append(";")
              .append(p.getCommande() != null ? p.getCommande().getNumeroTicket() : "-").append(";")
              .append(fmt(p.getMontant())).append(";")
              .append(p.getMoyenPaiement()).append(";")
              .append(p.getTypePaiement()).append(";")
              .append(p.getReferenceTransaction() != null ? p.getReferenceTransaction() : "").append("\n");
        }
        return sb.toString();
    }

    // ==================== CAISSE (clôtures) ====================

    @Transactional(readOnly = true)
    public byte[] caissePdf() {
        List<ClotureDTO> clotures = clotureService.historique();
        Document doc = new Document(PageSize.A4, 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(titre("Historique des clôtures de caisse"));
            PdfPTable t = new PdfPTable(7);
            t.setWidthPercentage(100);
            headerRow(t, "Date", "Clôturé le", "Total logiciel", "Total compte", "Écart", "Nb paiements", "Observations");
            for (ClotureDTO c : clotures) {
                t.addCell(cell(c.getDate() != null ? c.getDate().format(D) : ""));
                t.addCell(cell(c.getDateCloture() != null ? c.getDateCloture().format(DT) : ""));
                t.addCell(cell(fmt(c.getTotalLogiciel())));
                t.addCell(cell(fmt(c.getTotalCompte())));
                PdfPCell ecartCell = cell(fmt(c.getEcart()));
                if (c.getEcart() != null && c.getEcart().signum() != 0) {
                    ecartCell.setBackgroundColor(new java.awt.Color(255, 220, 220));
                }
                t.addCell(ecartCell);
                t.addCell(cell(String.valueOf(c.getNombrePaiements())));
                t.addCell(cell(c.getObservations() != null ? c.getObservations() : ""));
            }
            doc.add(t);
            footer(doc);
            doc.close();
        } catch (Exception e) { throw new RuntimeException("Erreur génération PDF caisse", e); }
        return out.toByteArray();
    }

    @Transactional(readOnly = true)
    public String caisseCsv() {
        List<ClotureDTO> clotures = clotureService.historique();
        StringBuilder sb = new StringBuilder();
        sb.append("Date;Cloture le;Total logiciel;Total compte;Ecart;Nb paiements;Observations\n");
        for (ClotureDTO c : clotures) {
            sb.append(c.getDate()).append(";")
              .append(c.getDateCloture() != null ? c.getDateCloture().format(DT) : "").append(";")
              .append(fmt(c.getTotalLogiciel())).append(";")
              .append(fmt(c.getTotalCompte())).append(";")
              .append(fmt(c.getEcart())).append(";")
              .append(c.getNombrePaiements()).append(";")
              .append(c.getObservations() != null ? c.getObservations() : "").append("\n");
        }
        return sb.toString();
    }

    // ==================== INVENTAIRE ====================

    @Transactional(readOnly = true)
    public byte[] inventairePdf() {
        List<Vetements> stocks = vetementsRepository.findAllByOrderByLibelleAsc();
        Document doc = new Document(PageSize.A4, 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(titre("Inventaire des vêtements"));
            PdfPTable t = new PdfPTable(3);
            t.setWidthPercentage(60);
            headerRow(t, "Libellé", "Type", "Stock");
            int totalStock = 0;
            for (Vetements v : stocks) {
                t.addCell(cell(v.getLibelle()));
                t.addCell(cell(v.getTypedevetement() != null ? v.getTypedevetement() : ""));
                t.addCell(cell(String.valueOf(v.getQuantiteStock())));
                totalStock += v.getQuantiteStock() != null ? v.getQuantiteStock() : 0;
            }
            PdfPCell sumCell = cell("TOTAL : " + totalStock);
            sumCell.setColspan(2);
            sumCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            sumCell.setPhrase(new Phrase("TOTAL : " + totalStock, boldFont(10)));
            t.addCell(sumCell);
            t.addCell(cell(String.valueOf(totalStock)));
            doc.add(t);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(" "));
            doc.add(titre("Bons d'inventaire récents"));
            List<Inventaire> bons = inventaireRepository.findAllByOrderByDateInventaireDesc();
            PdfPTable t2 = new PdfPTable(4);
            t2.setWidthPercentage(100);
            headerRow(t2, "#", "Date", "Observations", "Lignes");
            for (Inventaire b : bons) {
                t2.addCell(cell(String.valueOf(b.getIdinventaire())));
                t2.addCell(cell(b.getDateInventaire() != null ? b.getDateInventaire().format(D) : ""));
                t2.addCell(cell(b.getObservations() != null ? b.getObservations() : ""));
                int qte = b.getLignes().stream().mapToInt(l -> l.getQuantite() != null ? l.getQuantite() : 0).sum();
                t2.addCell(cell(qte + " pièce(s)"));
            }
            doc.add(t2);
            footer(doc);
            doc.close();
        } catch (Exception e) { throw new RuntimeException("Erreur génération PDF inventaire", e); }
        return out.toByteArray();
    }

    @Transactional(readOnly = true)
    public String inventaireCsv() {
        List<Vetements> stocks = vetementsRepository.findAllByOrderByLibelleAsc();
        StringBuilder sb = new StringBuilder();
        sb.append("Libellé;Type;Stock\n");
        for (Vetements v : stocks) {
            sb.append(v.getLibelle()).append(";")
              .append(v.getTypedevetement() != null ? v.getTypedevetement() : "").append(";")
              .append(v.getQuantiteStock()).append("\n");
        }
        return sb.toString();
    }

    // ==================== FIDÉLITÉ ====================

    @Transactional(readOnly = true)
    public byte[] fidelitePdf() {
        List<Client> clients = clientRepository.findAll();
        Document doc = new Document(PageSize.A4, 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(titre("Programme de fidélité — Points clients"));
            PdfPTable t = new PdfPTable(4);
            t.setWidthPercentage(80);
            headerRow(t, "Client", "Téléphone", "Points", "Valeur (FCFA)");
            int totalPts = 0;
            for (Client c : clients) {
                int pts = c.getPoints_fidelites() != null ? c.getPoints_fidelites() : 0;
                t.addCell(cell(c.getNom()));
                t.addCell(cell(c.getTelephone()));
                t.addCell(cell(String.valueOf(pts)));
                t.addCell(cell(String.valueOf(pts)));
                totalPts += pts;
            }
            PdfPCell sum = cell("TOTAL : " + totalPts + " pts");
            sum.setColspan(2);
            sum.setHorizontalAlignment(Element.ALIGN_RIGHT);
            sum.setPhrase(new Phrase("TOTAL : " + totalPts + " pts", boldFont(10)));
            t.addCell(sum);
            t.addCell(cell(String.valueOf(totalPts)));
            t.addCell(cell(String.valueOf(totalPts)));
            doc.add(t);
            footer(doc);
            doc.close();
        } catch (Exception e) { throw new RuntimeException("Erreur génération PDF fidélité", e); }
        return out.toByteArray();
    }

    @Transactional(readOnly = true)
    public String fideliteCsv() {
        List<Client> clients = clientRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Client;Telephone;Points;Valeur FCFA\n");
        for (Client c : clients) {
            int pts = c.getPoints_fidelites() != null ? c.getPoints_fidelites() : 0;
            sb.append(c.getNom()).append(";")
              .append(c.getTelephone()).append(";")
              .append(pts).append(";")
              .append(pts).append("\n");
        }
        return sb.toString();
    }

    // ==================== HELPERS PDF ====================

    private Paragraph titre(String text) {
        Paragraph p = new Paragraph(text, boldFont(14));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(12);
        return p;
    }

    private com.lowagie.text.Font boldFont(int size) {
        return new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, size, com.lowagie.text.Font.BOLD);
    }

    private com.lowagie.text.Font normalFont(int size) {
        return new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, size, com.lowagie.text.Font.NORMAL);
    }

    private void headerRow(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, boldFont(9)));
            cell.setBackgroundColor(new java.awt.Color(50, 100, 160));
            cell.setPhrase(new Phrase(h, new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE)));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4);
            table.addCell(cell);
        }
    }

    private PdfPCell cell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "", normalFont(9)));
        c.setPadding(3);
        return c;
    }

    private void footer(Document doc) {
        Paragraph p = new Paragraph("Imprimé le " + LocalDateTime.now().format(DT), normalFont(8));
        p.setAlignment(Element.ALIGN_RIGHT);
        p.setSpacingBefore(10);
        doc.add(p);
    }

    private String dateRange(LocalDate debut, LocalDate fin) {
        if (debut == null && fin == null) return "";
        StringBuilder sb = new StringBuilder(" (");
        if (debut != null) sb.append("du ").append(debut.format(D));
        if (fin != null) sb.append(" au ").append(fin.format(D));
        sb.append(")");
        return sb.toString();
    }

    private String fmt(BigDecimal val) {
        if (val == null) return "0";
        return val.setScale(0, java.math.RoundingMode.HALF_UP).toString();
    }
}
