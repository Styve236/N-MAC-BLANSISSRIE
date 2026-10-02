package com.pressing.pressing.api.repository;

import com.pressing.pressing.api.entite.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.userId = :userId and r.revoked = false")
    int revoquerToutesPourUtilisateur(@Param("userId") Long userId);

    /** Supprime les lignes heritees ou le jeton etait stocke en clair (empreinte SHA-256 = 64 caracteres). */
    @Modifying
    @Query("delete from RefreshToken r where length(r.token) <> 64")
    int supprimerJetonsNonHaches();
}
