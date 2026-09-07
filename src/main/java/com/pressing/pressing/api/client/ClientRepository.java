package com.pressing.pressing.api.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByTelephone(String telephone);

    List<Client> findByNomContainsIgnoreCase(String nom);

    Page<Client> findByNomContainsIgnoreCase(String nom, Pageable pageable);

    boolean existsByTelephone(String telephone);
}
