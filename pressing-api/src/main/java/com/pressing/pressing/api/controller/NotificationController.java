package com.pressing.pressing.api.controller;
import com.pressing.pressing.api.dto.response.NotificationDTO;
import com.pressing.pressing.api.security.UtilisateurPrincipal;
import com.pressing.pressing.api.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Tous les rôles connectés : leurs notifications internes (cibles par rôle)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','AGENT_PRODUCTION','LIVREUR')")
    public ResponseEntity<List<NotificationDTO>> mesNotifications(
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return ResponseEntity.ok(notificationService.mesNotifications(
                principal.getUsers().getIdusers(), principal.getUsers().getRole()));
    }

    // Nombre de notifications non lues (pour le badge)
    @GetMapping("/non-lues")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','AGENT_PRODUCTION','LIVREUR')")
    public ResponseEntity<Integer> compterNonLues(
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return ResponseEntity.ok(notificationService.compterNonLues(
                principal.getUsers().getIdusers(), principal.getUsers().getRole()));
    }

    // Marquer une notification comme lue
    @PatchMapping("/{id}/lue")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','AGENT_PRODUCTION','LIVREUR')")
    public ResponseEntity<Void> marquerLue(
            @PathVariable Long id,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        notificationService.marquerLue(id, principal.getUsers().getIdusers());
        return ResponseEntity.noContent().build();
    }

    // Marquer toutes les notifications comme lues
    @PatchMapping("/tout-lue")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','AGENT_PRODUCTION','LIVREUR')")
    public ResponseEntity<Void> toutLue(
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        notificationService.toutLue(principal.getUsers().getIdusers());
        return ResponseEntity.noContent().build();
    }
}