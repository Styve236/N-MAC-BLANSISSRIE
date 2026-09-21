package com.pressing.pressing.api.controller;
import com.pressing.pressing.api.dto.request.CommandeRequestDTO;
import com.pressing.pressing.api.dto.request.PaiementDTO;
import com.pressing.pressing.api.dto.response.CommandeDTO;
import com.pressing.pressing.api.dto.response.PaiementResponseDTO;
import com.pressing.pressing.api.entite.Commande;
import com.pressing.pressing.api.entite.StatutCommande;
import com.pressing.pressing.api.service.CommandeService;
import com.pressing.pressing.api.service.PaiementService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;





@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Commandes", description = "Creation, suivi et traitement des commandes")
public class CommandeController {
    private final CommandeService commandeService;
    private final PaiementService paiementService;

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
            @PageableDefault(size = 20, sort = "dateCreation", direction = Sort.Direction.DESC) Pageable pageable) {
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
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
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
