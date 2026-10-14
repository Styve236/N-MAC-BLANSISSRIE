package com.pressing.pressing.api.controller;
import com.pressing.pressing.api.dto.response.RecuDTO;
import com.pressing.pressing.api.dto.response.SuiviDTO;
import com.pressing.pressing.api.service.RecuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portail de suivi client (public). Comme la page de recu, il reste accessible
 * sans authentification mais n'accepte que le numero de ticket (long et non
 * devinable), ne renvoie que des champs non sensibles (telephone masque) et est
 * protege par le limiteur de tentatives.
 */
@RestController
@RequestMapping("/api/suivi")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Suivi client", description = "Suivi public d'une commande par numero de ticket")
public class SuiviController {

    private final RecuService recuService;

    @GetMapping("/{numeroTicket}")
    public ResponseEntity<SuiviDTO> suivre(@PathVariable String numeroTicket) {
        RecuDTO recu = recuService.getRecuParNumeroTicket(numeroTicket.trim().toUpperCase());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(toDTO(recu));
    }

    private SuiviDTO toDTO(RecuDTO recu) {
        SuiviDTO dto = new SuiviDTO();
        dto.setNumeroTicket(recu.getNumeroTicket());
        dto.setStatut(recu.getStatut());
        dto.setNomClient(recu.getNomClient());
        dto.setTelephoneMasque(masquer(recu.getTelephoneClient()));
        dto.setDateCreation(recu.getDateCreation());
        dto.setDateRecuperationPrevue(recu.getDateRecuperationPrevue());
        dto.setPoidsTotal(recu.getPoidsTotal());
        dto.setMontantTotal(recu.getMontantTotal());
        dto.setMontantPaye(recu.getMontantPaye());
        dto.setResteAPayer(recu.getResteAPayer());
        dto.setLignes(recu.getLignes());
        return dto;
    }

    private String masquer(String telephone) {
        if (telephone == null || telephone.isBlank()) {
            return null;
        }
        String chiffres = telephone.replaceAll("[^0-9]", "");
        if (chiffres.length() <= 4) {
            return "****";
        }
        return chiffres.substring(0, 2) + "****" + chiffres.substring(chiffres.length() - 2);
    }
}
