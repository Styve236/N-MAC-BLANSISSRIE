package com.pressing.pressing.api.controller;
import com.pressing.pressing.api.dto.response.ClientDTO;
import com.pressing.pressing.api.service.ClientService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;




@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Clients", description = "Gestion des clients et recherche")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<ClientDTO> creer(@Valid @RequestBody ClientDTO dto) {
        ClientDTO cree = clientService.creerClient(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(cree);
    }

    // L'annuaire complet est reserve a l'accueil et a l'administration : sans cette
    // restriction, un agent de production ou un livreur pouvait enumerer tout le
    // carnet client (noms, quartiers, telephones, points de fidelite) alors que
    // l'interface ne leur affiche aucun de ces ecrans.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public Page<ClientDTO> listerTous(
            @PageableDefault(size = 20, sort = "nom") Pageable pageable) {
        return clientService.listerTousPaginer(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<ClientDTO> consulterParId(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.consulterParId(id));
    }

    // Recherche ciblee : necessaire a l'agent de production quand il enregistre
    // une commande pour un client existant, et au livreur pour identifier la livraison.
    @GetMapping("/recherche/telephone")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','AGENT_PRODUCTION','LIVREUR')")
    public ResponseEntity<ClientDTO> rechercherParTelephone(@RequestParam String telephone) {
        return ResponseEntity.ok(clientService.rechercherParTelephone(telephone));
    }

    @GetMapping("/recherche/nom")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','AGENT_PRODUCTION','LIVREUR')")
    public Page<ClientDTO> rechercherParNom(
            @RequestParam String nom,
            @PageableDefault(size = 20) Pageable pageable) {
        return clientService.rechercherParNomPaginer(nom, pageable);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        clientService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
