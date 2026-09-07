package com.pressing.pressing.api.client;

import com.pressing.pressing.api.common.dto.ClientDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<ClientDTO> creer(@Valid @RequestBody ClientDTO dto) {
        ClientDTO cree = clientService.creerClient(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(cree);
    }

    @GetMapping
    public Page<ClientDTO> listerTous(
            @PageableDefault(size = 20, sort = "nom") Pageable pageable) {
        return clientService.listerTousPaginer(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> consulterParId(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.consulterParId(id));
    }

    @GetMapping("/recherche/telephone")
    public ResponseEntity<ClientDTO> rechercherParTelephone(@RequestParam String telephone) {
        return ResponseEntity.ok(clientService.rechercherParTelephone(telephone));
    }

    @GetMapping("/recherche/nom")
    public Page<ClientDTO> rechercherParNom(
            @RequestParam String nom,
            @PageableDefault(size = 20) Pageable pageable) {
        return clientService.rechercherParNomPaginer(nom, pageable);
    }
}
