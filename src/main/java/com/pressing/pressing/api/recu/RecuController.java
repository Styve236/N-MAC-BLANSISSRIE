package com.pressing.pressing.api.recu;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/commandes/{id}/recu")
@RequiredArgsConstructor
public class RecuController {

    private final RecuService recuService;

    @GetMapping
    public RecuDTO consulter(@PathVariable Long id) {
        return recuService.getRecuParCommandeId(id);
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] data = recuService.getRecuPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recu-commande-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @PostMapping("/envoyer")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public RecuDTO envoyerParSms(@PathVariable Long id) {
        return recuService.envoyerRecuParSms(id);
    }
}
