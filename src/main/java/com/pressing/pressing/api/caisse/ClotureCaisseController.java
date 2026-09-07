package com.pressing.pressing.api.caisse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/caisse")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
public class ClotureCaisseController {

    private final ClotureService clotureService;

    @GetMapping("/rapport")
    public ResponseEntity<ClotureDTO> rapport(@RequestParam(required = false) String date) {
        return ResponseEntity.ok(clotureService.rapport(parsingDate(date)));
    }

    @PostMapping("/cloturer")
    public ResponseEntity<ClotureDTO> cloturer(@RequestBody ClotureRequestDTO requete) {
        return ResponseEntity.ok(clotureService.cloturer(requete));
    }

    @GetMapping("/clotures")
    public ResponseEntity<List<ClotureDTO>> historique() {
        return ResponseEntity.ok(clotureService.historique());
    }

    @GetMapping(value = "/ticket", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> ticket(@RequestParam(required = false) String date) {
        LocalDate jour = parsingDate(date);
        ClotureDTO dto = clotureService.rapport(jour);
        return ResponseEntity.ok(htmlTicket(dto));
    }

    private LocalDate parsingDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(date);
    }

    private String htmlTicket(ClotureDTO dto) {
        StringBuilder moyens = new StringBuilder();
        if (dto.getParMoyen() != null) {
            for (MontantParMoyenDTO m : dto.getParMoyen()) {
                moyens.append("<tr><td>").append(m.getMoyen())
                        .append("</td><td style='text-align:right'>").append(m.getNombre())
                        .append("</td><td style='text-align:right'>").append(format(m.getMontant()))
                        .append("</td></tr>");
            }
        }
        String etat = dto.isCloturee()
                ? "<div class='ligne'><span>Clôture :</span><b style='color:#0a7d33'>✔ " + format(dto.getTotalCompte()) + " (écart " + format(dto.getEcart()) + ")</b></div>"
                : "<div class='ligne'><span>Clôture :</span><b style='color:#b3261e'>Non clôturée</b></div>";
        return "<!DOCTYPE html><html lang='fr'><head><meta charset='utf-8'><title>Ticket de caisse " + dto.getDate() + "</title>"
                + "<style>body{font-family:monospace;max-width:340px;margin:20px auto;padding:16px;border:1px dashed #888}"
                + "h2{font-size:15px;margin:0 0 6px;text-align:center}.centre{text-align:center}.ligne{display:flex;justify-content:space-between;margin:2px 0}"
                + "table{width:100%;border-collapse:collapse;margin:8px 0}td,th{border-top:1px dashed #ccc;padding:3px 2px;font-size:13px}"
                + "th{font-size:12px;color:#555;border-top:none}.total{font-weight:bold;border-top:2px solid #333}.fade{color:#777}</style></head><body>"
                + "<h2>PRESSING CHERIF</h2>"
                + "<div class='centre fade'>Ticket de clôture de caisse</div>"
                + "<div class='centre'>" + dto.getDate() + " — " + dto.getNombrePaiements() + " paiement(s)</div><hr>"
                + "<table><tr><th>Moyen</th><th>Nombre</th><th>Montant</th></tr>" + moyens + "</table>"
                + "<div class='ligne total'><span>Total encaissé :</span><span>" + format(dto.getTotalLogiciel()) + " FCFA</span></div>"
                + etat
                + (dto.getObservations() != null && !dto.getObservations().isBlank()
                        ? "<div class='ligne'><span>Observations :</span><span>" + dto.getObservations() + "</span></div>" : "")
                + "<hr><div class='centre fade'>Imprimé le " + java.time.LocalDateTime.now() + "</div>"
                + "<script>window.print()</script></body></html>";
    }

    private String format(BigDecimal montant) {
        return montant == null ? "0" : montant.setScale(0, java.math.RoundingMode.HALF_UP).toString();
    }
}