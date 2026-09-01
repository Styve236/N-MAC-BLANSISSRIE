package com.pressing.pressing.api.commande;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tarifs")
@RequiredArgsConstructor
public class TarifController {

    private final TarifService tarifService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Tarif creer(@RequestBody Tarif tarif) {
        return tarifService.creer(tarif);
    }

    @GetMapping
    public List<Tarif> lister(@RequestParam(required = false) TypeNettoyage typeNettoyage) {
        return tarifService.lister(typeNettoyage);
    }

    @GetMapping("/actifs")
    public List<Tarif> listerActifs() {
        return tarifService.listerActifs();
    }

    @GetMapping("/{id}")
    public Tarif consulter(@PathVariable Long id) {
        return tarifService.consulter(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Tarif modifier(@PathVariable Long id, @RequestBody Tarif tarif) {
        return tarifService.modifier(id, tarif);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void desactiver(@PathVariable Long id) {
        tarifService.desactiver(id);
    }

    @PatchMapping("/{id}/activer")
    @PreAuthorize("hasRole('ADMIN')")
    public Tarif activer(@PathVariable Long id) {
        return tarifService.activer(id);
    }
}