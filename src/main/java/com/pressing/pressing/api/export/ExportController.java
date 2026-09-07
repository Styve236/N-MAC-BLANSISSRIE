package com.pressing.pressing.api.export;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
public class ExportController {

    private final ExportService exportService;

    // --- Commandes ---
    @GetMapping(value = "/commandes", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> commandesPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return pdf(exportService.commandesPdf(debut, fin), "commandes.pdf");
    }

    @GetMapping(value = "/commandes/csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> commandesCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return csv(exportService.commandesCsv(debut, fin), "commandes.csv");
    }

    // --- Paiements ---
    @GetMapping(value = "/paiements", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> paiementsPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return pdf(exportService.paiementsPdf(debut, fin), "paiements.pdf");
    }

    @GetMapping(value = "/paiements/csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> paiementsCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return csv(exportService.paiementsCsv(debut, fin), "paiements.csv");
    }

    // --- Caisse ---
    @GetMapping(value = "/caisse", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> caissePdf() {
        return pdf(exportService.caissePdf(), "caisse.pdf");
    }

    @GetMapping(value = "/caisse/csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> caisseCsv() {
        return csv(exportService.caisseCsv(), "caisse.csv");
    }

    // --- Inventaire ---
    @GetMapping(value = "/inventaire", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> inventairePdf() {
        return pdf(exportService.inventairePdf(), "inventaire.pdf");
    }

    @GetMapping(value = "/inventaire/csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> inventaireCsv() {
        return csv(exportService.inventaireCsv(), "inventaire.csv");
    }

    // --- Fidélité ---
    @GetMapping(value = "/fidelite", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> fidelitePdf() {
        return pdf(exportService.fidelitePdf(), "fidelite.pdf");
    }

    @GetMapping(value = "/fidelite/csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> fideliteCsv() {
        return csv(exportService.fideliteCsv(), "fidelite.csv");
    }

    // --- Helpers ---
    private ResponseEntity<byte[]> pdf(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    private ResponseEntity<String> csv(String data, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }
}
