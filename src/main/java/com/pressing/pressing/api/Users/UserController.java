package com.pressing.pressing.api.Users;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Users creer(@RequestBody @Valid UserRequestDTO dto) {
        return userService.creer(dto);
    }

    @GetMapping
    public List<Users> lister() {
        return userService.lister();
    }

    @GetMapping("/{id}")
    public Users consulter(@PathVariable Long id) {
        return userService.consulter(id);
    }

    @PutMapping("/{id}")
    public Users modifier(@PathVariable Long id, @RequestBody @Valid UserRequestDTO dto) {
        return userService.modifier(id, dto);
    }

    @PatchMapping("/{id}/statut")
    public Users changerStatut(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return userService.changerStatut(id, Boolean.TRUE.equals(body.get("actif")));
    }

    @PostMapping("/{id}/mot-de-passe")
    public Users reinitialiserMotDePasse(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return userService.reinitialiserMotDePasse(id, body.get("password"));
    }
}