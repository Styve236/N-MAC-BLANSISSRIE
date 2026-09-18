package com.pressing.pressing.api.controller;
import com.pressing.pressing.api.dto.request.UserRequestDTO;
import com.pressing.pressing.api.dto.response.UserDTO;
import com.pressing.pressing.api.service.UserService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;




@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO creer(@RequestBody @Valid UserRequestDTO dto) {
        return userService.creer(dto);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDTO> lister() {
        return userService.lister();
    }

    @GetMapping("/livreurs")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','AGENT_PRODUCTION','LIVREUR')")
    public List<UserDTO> listerLivreurs() {
        return userService.listerLivreurs();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO consulter(@PathVariable Long id) {
        return userService.consulter(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO modifier(@PathVariable Long id, @RequestBody @Valid UserRequestDTO dto) {
        return userService.modifier(id, dto);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO changerStatut(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return userService.changerStatut(id, Boolean.TRUE.equals(body.get("actif")));
    }

    @PostMapping("/{id}/mot-de-passe")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO reinitialiserMotDePasse(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return userService.reinitialiserMotDePasse(id, body.get("password"));
    }
}
