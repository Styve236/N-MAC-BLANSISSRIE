package com.pressing.pressing.api.client;

import com.pressing.pressing.api.common.dto.ClientDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    // CREER un client
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<ClientDTO> creer(@Valid @RequestBody ClientDTO dto) {
        ClientDTO cree = clientService.creerClient(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(cree);
    }

    // CONSULTER tous les clients
    @GetMapping
    public ResponseEntity<List<ClientDTO>> listerTous() {
        return ResponseEntity.ok(clientService.listerTous());
    }

    // CONSULTER un client par son id
    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> consulterParId(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.consulterParId(id));
    }

    // RECHERCHER un client par téléphone
    @GetMapping("/recherche/telephone")
    public ResponseEntity<ClientDTO> rechercherParTelephone(@RequestParam String telephone) {
        return ResponseEntity.ok(clientService.rechercherParTelephone(telephone));
    }

    // RECHERCHER des clients par nom
    @GetMapping("/recherche/nom")
    public ResponseEntity<List<ClientDTO>> rechercherParNom(@RequestParam String nom) {
        return ResponseEntity.ok(clientService.rechercherParNom(nom));
    }
}
