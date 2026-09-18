package com.pressing.pressing.api.controller;
import com.pressing.pressing.api.dto.response.PaiementResponseDTO;
import com.pressing.pressing.api.dto.response.StatutPaiementDTO;
import com.pressing.pressing.api.service.PaiementService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;




@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
public class PaiementController {

    private final PaiementService paiementService;

    @GetMapping("/api/paiements/ca-du-jour")
    public BigDecimal caDuJour() {
        return paiementService.getCAduJour(LocalDate.now());
    }

    @GetMapping("/api/paiements")
    public Page<PaiementResponseDTO> lister(
            @RequestParam(required = false) Long clientId,
            @PageableDefault(size = 20, sort = "datePaiement", direction = Sort.Direction.DESC) Pageable pageable) {
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
