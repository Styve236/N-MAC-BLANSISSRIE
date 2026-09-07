package com.pressing.pressing.api.commande;


import com.pressing.pressing.api.common.dto.PaiementDTO;
import com.pressing.pressing.api.common.dto.CommandeRequestDTO;
import com.pressing.pressing.api.common.dto.CommandeDTO;
import com.pressing.pressing.api.common.dto.PaiementResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
public class CommandeController {
    private final CommandeService commandeService;
    private final com.pressing.pressing.api.Paiement.PaiementService paiementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public Commande creer(@RequestBody CommandeRequestDTO dto){
        return commandeService.creerCommande(dto);
    }

    @GetMapping
    public Page<Commande> lister(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) StatutCommande statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @PageableDefault(size = 20, sort = "dateCreation", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return commandeService.listerPaginer(clientId, statut, dateDebut, dateFin, pageable);
    }

    @GetMapping("/{id}")
    public Commande consulter(@PathVariable Long id) {
        return commandeService.consulter(id);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','AGENT_PRODUCTION','LIVREUR')")
    public Commande changerStatut(@PathVariable Long id, @RequestBody Map<String, StatutCommande> body) {
        return commandeService.changerStatut(id, body.get("statut"));
    }

    @GetMapping("/client/{clientId}")
    public List<CommandeDTO> historiqueParClient(@PathVariable Long clientId){
        return commandeService.getHistoriqueParClient(clientId);
    }

    @PostMapping("/{id}/payer")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public PaiementResponseDTO payer(@PathVariable Long id, @RequestBody PaiementDTO dto){
        return paiementService.ajouter(id, dto);
    }

    @GetMapping("/{id}/payer")
    public BigDecimal reste(@PathVariable Long id)
    {
        return commandeService.getResteAPayer(id);
    }

    @PostMapping("/{id}/pret")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','AGENT_PRODUCTION')")
    public Commande marquerPret(@PathVariable Long id){
        return commandeService.marquerPret(id);
    }

    @PostMapping("/rappel-impayes")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public int rappelImpayes(){
        return commandeService.envoyerRappelImpayes();
    }
}
