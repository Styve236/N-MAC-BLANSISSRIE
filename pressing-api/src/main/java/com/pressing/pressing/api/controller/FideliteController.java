package com.pressing.pressing.api.controller;
import com.pressing.pressing.api.dto.request.CrediterPointsRequestDTO;
import com.pressing.pressing.api.dto.request.UtiliserPointsRequestDTO;
import com.pressing.pressing.api.entite.HistoriquePoint;
import com.pressing.pressing.api.service.FideliteService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;




@RestController
@RequestMapping("/api/fidelite")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Fidelite", description = "Points de fidelite des clients")
public class FideliteController {

    private final FideliteService fideliteService;

    @GetMapping("/solde/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<Map<String, Object>> solde(@PathVariable Long clientId) {
        int points = fideliteService.soldePoints(clientId);
        return ResponseEntity.ok(Map.of(
                "clientId", clientId,
                "points", points,
                "valeurEstimee", points + " FCFA"
        ));
    }

    @GetMapping("/historique/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<List<HistoriquePoint>> historique(@PathVariable Long clientId) {
        return ResponseEntity.ok(fideliteService.historiquePoints(clientId));
    }

    @PostMapping("/utiliser")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<Map<String, Object>> utiliserPoints(@RequestBody UtiliserPointsRequestDTO requete) {
        BigDecimal remise = fideliteService.utiliserPoints(
                requete.getClientId(),
                requete.getCommandeId(),
                requete.getNbPoints());
        return ResponseEntity.ok(Map.of(
                "remise", remise,
                "nbPointsUtilises", requete.getNbPoints(),
                "message", "Remise de " + remise + " FCFA appliquée"
        ));
    }

    @PostMapping("/crediter")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> crediter(@RequestBody CrediterPointsRequestDTO requete) {
        fideliteService.crediterPointsManuellement(requete.getClientId(), requete.getNbPoints(), requete.getDescription());
        return ResponseEntity.ok(Map.of(
                "clientId", requete.getClientId(),
                "nbPoints", requete.getNbPoints(),
                "message", requete.getNbPoints() + " points crédités"
        ));
    }
}