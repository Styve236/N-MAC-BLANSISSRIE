package com.pressing.pressing.api.commande;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarifRepository extends JpaRepository<Tarif, Long> {

    //1.Listers tous les tarifs du pressing
    List<Tarif> findByActifTrue();

    //2.Chercher un tarif par nom
    List<Tarif> findByNomContainingIgnoreCase(String nom);

    //3.pour les types de vetements
    List<Tarif> findByTypevetementContainingIgnoreCase(String typevetement);

    //4. les tarifs d'un type de nettoyage
    List<Tarif> findByTypeNettoyage(TypeNettoyage typeNettoyage);

    //5. les tarifs actifs d'un type de nettoyage
    List<Tarif> findByTypeNettoyageAndActifTrue(TypeNettoyage typeNettoyage);
}
