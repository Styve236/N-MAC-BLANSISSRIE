package com.pressing.pressing.api.inventaire;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventaire")
@RequiredArgsConstructor
public class InventaireController {

    private final InventaireService inventaireService;

    // ---------- Lectures (ADMIN + RECEPTIONNISTE) ----------

    @GetMapping("/vetements")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<List<VetementDTO>> listeVetements() {
        return ResponseEntity.ok(inventaireService.listeVetements());
    }

    @GetMapping("/bons")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<List<BonInventaireDTO>> listeBons() {
        return ResponseEntity.ok(inventaireService.listeBons());
    }

    @GetMapping("/bons/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<BonInventaireDTO> getBon(@PathVariable Long id) {
        return ResponseEntity.ok(inventaireService.getBon(id));
    }

    // ---------- Écritures (ADMIN uniquement) ----------

    @PostMapping("/vetements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VetementDTO> creerVetement(@RequestBody VetementDTO dto) {
        return ResponseEntity.ok(inventaireService.creerVetement(dto));
    }

    @PutMapping("/vetements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VetementDTO> modifierVetement(@PathVariable Long id, @RequestBody VetementDTO dto) {
        return ResponseEntity.ok(inventaireService.modifierVetement(id, dto));
    }

    @DeleteMapping("/vetements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> supprimerVetement(@PathVariable Long id) {
        inventaireService.supprimerVetement(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BonInventaireDTO> creerBon(@RequestBody BonInventaireRequestDTO requete) {
        return ResponseEntity.ok(inventaireService.creerBon(requete));
    }
}