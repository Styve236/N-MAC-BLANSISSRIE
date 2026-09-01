package com.pressing.pressing.api.client;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    //rechercher un client a l'aide de son numero de telephone

    Optional<Client> findByTelephone(String telephone);

    //methode pour la recherche par le nom
List<Client> findByNomContainsIgnoreCase(String nom);

    //methode pour verifier si ke telephone existe deja dans la bd
    boolean existsByTelephone(String telephone);
}
