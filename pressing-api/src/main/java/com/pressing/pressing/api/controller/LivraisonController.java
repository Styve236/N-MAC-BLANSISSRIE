package com.pressing.pressing.api.controller;
import com.pressing.pressing.api.dto.request.LivraisonRequestDTO;
import com.pressing.pressing.api.dto.response.LivraisonDTO;
import com.pressing.pressing.api.entite.StatutLivraison;
import com.pressing.pressing.api.security.UtilisateurPrincipal;
import com.pressing.pressing.api.service.LivraisonService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;




@RestController
@RequestMapping("/api/livraisons")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Livraisons", description = "Planification et suivi des livraisons")
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
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
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

    // LIVREUR : retirer une livraison livrée de son flux (l'admin la voit toujours)
    @PatchMapping("/{id}/masquer")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','LIVREUR')")
    public ResponseEntity<LivraisonDTO> masquer(
            @PathVariable Long id) {
        return ResponseEntity.ok(livraisonService.masquerLivraison(id));
    }
}