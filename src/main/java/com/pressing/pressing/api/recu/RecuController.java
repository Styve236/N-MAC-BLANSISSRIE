package com.pressing.pressing.api.recu;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/commandes/{id}/recu")
@RequiredArgsConstructor
public class RecuController {

    private final RecuService recuService;

    @GetMapping
    public RecuDTO consulter(@PathVariable Long id) {
        return recuService.getRecuParCommandeId(id);
    }

    @PostMapping("/envoyer")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public RecuDTO envoyerParSms(@PathVariable Long id) {
        return recuService.envoyerRecuParSms(id);
    }
}