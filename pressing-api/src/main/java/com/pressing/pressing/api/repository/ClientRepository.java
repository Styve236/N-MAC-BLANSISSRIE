package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.Client;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByTelephone(String telephone);

    Page<Client> findByNomContainsIgnoreCase(String nom, Pageable pageable);

    boolean existsByTelephone(String telephone);
}
