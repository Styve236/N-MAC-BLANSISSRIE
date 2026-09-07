package com.pressing.pressing.api.commande;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarifRepository extends JpaRepository<Tarif, Long> {

    List<Tarif> findByActifTrue();

    Page<Tarif> findByActifTrue(Pageable pageable);

    List<Tarif> findByNomContainingIgnoreCase(String nom);

    List<Tarif> findByTypevetementContainingIgnoreCase(String typevetement);

    List<Tarif> findByTypeNettoyage(TypeNettoyage typeNettoyage);

    Page<Tarif> findByTypeNettoyage(TypeNettoyage typeNettoyage, Pageable pageable);

    List<Tarif> findByTypeNettoyageAndActifTrue(TypeNettoyage typeNettoyage);
}
