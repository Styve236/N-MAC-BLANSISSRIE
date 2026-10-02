package com.pressing.pressing.api.repository;

import com.pressing.pressing.api.entite.Commande;
import com.pressing.pressing.api.entite.StatutCommande;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Criteres de recherche des commandes, appliques par la base de donnees.
 * Ils remplacent le filtrage "charge tout puis filtre en Java" qui saturait la memoire
 * des que le volume de commandes augmentait.
 */
public final class CommandeSpecification {

    private CommandeSpecification() {
    }

    public static Specification<Commande> filtre(Long clientId, StatutCommande statut,
            LocalDate dateDebut, LocalDate dateFin) {
        return (racine, requete, cb) -> {
            List<Predicate> predicats = new ArrayList<>();

            if (clientId != null) {
                predicats.add(cb.equal(racine.get("client").get("idclient"), clientId));
            }
            if (statut != null) {
                predicats.add(cb.equal(racine.get("statut"), statut));
            }
            if (dateDebut != null) {
                predicats.add(cb.greaterThanOrEqualTo(racine.get("dateCreation"), dateDebut.atStartOfDay()));
            }
            if (dateFin != null) {
                predicats.add(cb.lessThanOrEqualTo(racine.get("dateCreation"), dateFin.atTime(23, 59, 59)));
            }

            return predicats.isEmpty() ? cb.conjunction() : cb.and(predicats.toArray(new Predicate[0]));
        };
    }
}
