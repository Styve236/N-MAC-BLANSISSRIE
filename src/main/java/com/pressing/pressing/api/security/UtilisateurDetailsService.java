package com.pressing.pressing.api.security;

import com.pressing.pressing.api.Users.UserRepository;
import com.pressing.pressing.api.Users.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UtilisateurDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Users utilisateur = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Aucun compte trouvé pour : " + email));

        return new UtilisateurPrincipal(utilisateur);
    }
}
