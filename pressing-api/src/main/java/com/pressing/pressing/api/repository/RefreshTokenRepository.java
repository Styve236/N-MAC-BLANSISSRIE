package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;



public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
}
