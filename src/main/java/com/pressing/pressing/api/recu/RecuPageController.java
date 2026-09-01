package com.pressing.pressing.api.recu;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/recu")
@RequiredArgsConstructor
public class RecuPageController {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final RecuService recuService;

    @GetMapping(value = "/{numeroTicket}", produces = MediaType.TEXT_HTML_VALUE)
    public String page(@PathVariable String numeroTicket) {
        RecuDTO recu = recuService.getRecuParNumeroTicket(numeroTicket);
        return html(recu);
    }

    private String html(RecuDTO recu) {
        StringBuilder lignes = new StringBuilder();
        for (LigneRecuDTO ligne : recu.getLignes()) {
            lignes.append("<tr>")
                    .append(td(esc(ligne.getDesignation())))
                    .append(td(esc(ligne.getTypeNettoyage())))
                    .append(td(esc(ligne.getUnite())))
                    .append(td(valeur(ligne.getQuantite())))
                    .append(td(formater(ligne.getPoids())))
                    .append(td(formater(ligne.getMontant())))
                    .append("</tr>");
        }
        if (recu.getLignes().isEmpty()) {
            lignes.append("<tr><td colspan=\"6\" style=\"text-align:center;color:#999\">Aucune ligne</td></tr>");
        }

        StringBuilder paiements = new StringBuilder();
        for (PaiementRecuDTO paiement : recu.getPaiements()) {
            paiements.append("<tr>")
                    .append(td(esc(paiement.getMoyenPaiement())))
                    .append(td(paiement.getDatePaiement() != null ? paiement.getDatePaiement().format(FORMAT_DATE) : ""))
                    .append(td(formater(paiement.getMontant())))
                    .append("</tr>");
        }
        if (recu.getPaiements().isEmpty()) {
            paiements.append("<tr><td colspan=\"3\" style=\"text-align:center;color:#999\">Aucun paiement enregistré</td></tr>");
        }

        return "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"><title>Reçu " + esc(recu.getNumeroTicket()) + "</title>"
                + "<style>"
                + "@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap');"
                + "body{font-family:'Inter',Arial,sans-serif;background:#f3f4f6;margin:0;padding:24px;color:#111827}"
                + ".feuille{max-width:520px;margin:0 auto;background:#fff;border-radius:12px;padding:28px;box-shadow:0 4px 16px rgba(0,0,0,.08)}"
                + ".entete{display:flex;justify-content:space-between;align-items:center;border-bottom:2px dashed #d1d5db;padding-bottom:14px}"
                + ".titre{font-size:20px;font-weight:700;color:#1f2937}.ticket{text-align:right;font-size:14px;color:#4b5563}"
                + ".ticket b{font-size:18px;color:#111827}"
                + ".infos{margin:14px 0;font-size:14px;line-height:1.6;color:#374151}"
                + "table{width:100%;border-collapse:collapse;font-size:13px;margin:12px 0}"
                + "th{background:#f9fafb;text-align:left;padding:8px;color:#4b5563;border-bottom:1px solid #e5e7eb}"
                + "td{padding:8px;border-bottom:1px solid #f3f4f6}"
                + ".montants{margin:14px 0;font-size:14px}"
                + ".ligne-total{display:flex;justify-content:space-between;padding:4px 0}"
                + ".total{display:flex;justify-content:space-between;font-weight:700;font-size:17px;border-top:2px solid #111827;padding-top:8px;margin-top:6px}"
                + ".restant{background:#fef2f2;color:#b91c1c;border-radius:8px;padding:10px;display:flex;justify-content:space-between;font-weight:600;margin-top:12px}"
                + ".paye{background:#ecfdf5;color:#047857;border-radius:8px;padding:10px;display:flex;justify-content:space-between;font-weight:600;margin-top:12px}"
                + "h3{font-size:14px;color:#374151;margin:18px 0 4px}"
                + ".pied{text-align:center;color:#6b7280;font-size:12px;margin-top:16px}"
                + ".btn-print{display:block;width:100%;margin:18px 0 6px;padding:12px;background:#111827;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer}"
                + "@media print{.btn-print{display:none}body{background:#fff;padding:0}.feuille{box-shadow:none}}"
                + "</style></head><body>"
                + "<div class=\"feuille\">"
                + "<button class=\"btn-print\" onclick=\"window.print()\">Imprimer ce reçu</button>"
                + "<div class=\"entete\"><div class=\"titre\">Pressing</div><div class=\"ticket\">N° <b>" + esc(recu.getNumeroTicket()) + "</b><br>Statut : " + esc(recu.getStatut()) + "</div></div>"
                + "<div class=\"infos\">"
                + "<strong>Client :</strong> " + esc(recu.getNomClient()) + "<br>"
                + "<strong>Téléphone :</strong> " + esc(recu.getTelephoneClient()) + "<br>"
                + "<strong>Déposé le :</strong> " + (recu.getDateCreation() != null ? recu.getDateCreation().format(FORMAT_DATE) : "") + "<br>"
                + "<strong>Retrait prévu :</strong> " + (recu.getDateRecuperationPrevue() != null ? recu.getDateRecuperationPrevue().format(FORMAT_DATE) : "-")
                + "</div>"
                + "<table><thead><tr><th>Prestation</th><th>Nettoyage</th><th>Unité</th><th>Qte</th><th>Poids</th><th>Montant</th></tr></thead><tbody>"
                + lignes
                + "</tbody></table>"
                + "<div class=\"montants\">"
                + "<div class=\"ligne-total\"><span>Poids total</span><span>" + formater(recu.getPoidsTotal()) + " kg</span></div>"
                + "<div class=\"ligne-total\"><span>Montant payé</span><span>" + formater(recu.getMontantPaye()) + " F CFA</span></div>"
                + "</div>"
                + "<div class=\"total\"><span>Total à payer</span><span>" + formater(recu.getMontantTotal()) + " F CFA</span></div>"
                + (recu.getResteAPayer() != null && recu.getResteAPayer().compareTo(BigDecimal.ZERO) > 0
                        ? "<div class=\"restant\"><span>Reste à payer</span><span>" + formater(recu.getResteAPayer()) + " F CFA</span></div>"
                        : "<div class=\"paye\"><span>Paiement</span><span>Soldé</span></div>")
                + "<h3>Paiements</h3>"
                + "<table><thead><tr><th>Moyen</th><th>Date</th><th>Montant</th></tr></thead><tbody>"
                + paiements
                + "</tbody></table>"
                + "<div class=\"pied\">Merci de votre confiance. Ce reçu est généré automatiquement.</div>"
                + "</div></body></html>";
    }

    private String td(String contenu) {
        return "<td>" + contenu + "</td>";
    }

    private String esc(String valeur) {
        if (valeur == null) {
            return "";
        }
        return valeur.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String valeur(Integer valeur) {
        return valeur != null ? String.valueOf(valeur) : "";
    }

    private String formater(BigDecimal valeur) {
        if (valeur == null) {
            return "0";
        }
        return valeur.stripTrailingZeros().toPlainString();
    }
}