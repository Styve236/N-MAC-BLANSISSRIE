package com.pressing.pressing.api.livraison;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/livraisons")
@RequiredArgsConstructor
public class LivraisonController {

    private final LivraisonService livraisonService;

    // ADMIN / RECEPTIONNISTE : voir toutes les livraisons
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<List<LivraisonDTO>> listerLivraisons(
            @RequestParam(required = false) StatutLivraison statut) {
        return ResponseEntity.ok(livraisonService.listerLivraisons(statut));
    }

    // LIVREUR : voir ses propres livraisons
    @GetMapping("/mes-livraisons")
    @PreAuthorize("hasRole('LIVREUR')")
    public ResponseEntity<List<LivraisonDTO>> mesLivraisons(
            @RequestParam(required = false) StatutLivraison statut,
            @AuthenticationPrincipal com.pressing.pressing.api.security.UtilisateurPrincipal principal) {
        return ResponseEntity.ok(livraisonService.mesLivraisons(principal.getUsers().getIdusers(), statut));
    }

    // ADMIN / RECEPTIONNISTE : assigner livraison
    @PostMapping("/{id}/assigner")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<LivraisonDTO> assignerLivraison(
            @PathVariable Long id,
            @RequestBody LivraisonRequestDTO requete) {
        return ResponseEntity.ok(livraisonService.assignerLivraison(id, requete));
    }

    // Tous les rôles autorisés : changer statut livraison
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','LIVREUR')")
    public ResponseEntity<LivraisonDTO> changerStatut(
            @PathVariable Long id,
            @RequestBody LivraisonRequestDTO requete) {
        StatutLivraison statut = requete.getStatut();
        return ResponseEntity.ok(livraisonService.changerStatutLivraison(id, statut));
    }
}