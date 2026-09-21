package com.pressing.pressing.api.controller;
import com.pressing.pressing.api.dto.response.RecuDTO;
import com.pressing.pressing.api.service.RecuService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;



@RestController
@RequestMapping("/api/commandes/{id}/recu")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Recus", description = "Generation et consultation des recus")
public class RecuController {

    private final RecuService recuService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public RecuDTO consulter(@PathVariable Long id) {
        return recuService.getRecuParCommandeId(id);
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
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
