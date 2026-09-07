package com.pressing.pressing.api.Paiement;

import com.pressing.pressing.api.common.dto.PaiementResponseDTO;
import com.pressing.pressing.api.common.dto.StatutPaiementDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaiementController {

    private final PaiementService paiementService;

    @GetMapping("/api/paiements/ca-du-jour")
    public BigDecimal caDuJour() {
        return paiementService.getCAduJour(LocalDate.now());
    }

    @GetMapping("/api/paiements")
    public Page<PaiementResponseDTO> lister(
            @RequestParam(required = false) Long clientId,
            @PageableDefault(size = 20, sort = "datePaiement", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return paiementService.listerTous(clientId, pageable);
    }

    @GetMapping("/api/commandes/{id}/paiements")
    public List<PaiementResponseDTO> paiementsCommande(@PathVariable Long id) {
        return paiementService.listerParCommande(id);
    }

    @GetMapping("/api/commandes/{id}/statut-paiement")
    public StatutPaiementDTO statutPaiement(@PathVariable Long id) {
        return paiementService.statutPaiement(id);
    }

    @GetMapping("/api/clients/{id}/paiements")
    public List<PaiementResponseDTO> paiementsClient(@PathVariable Long id) {
        return paiementService.listerParClient(id);
    }
}
