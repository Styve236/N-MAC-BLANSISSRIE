package com.pressing.pressing.api.commande;

import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TarifService {

    private final TarifRepository tarifRepository;

    public Tarif creer(Tarif tarif) {
        valider(tarif);
        if (tarif.getActif() == null) {
            tarif.setActif(true);
        }
        return tarifRepository.save(tarif);
    }

    public List<Tarif> lister(TypeNettoyage typeNettoyage) {
        if (typeNettoyage != null) {
            return tarifRepository.findByTypeNettoyage(typeNettoyage);
        }
        return tarifRepository.findAll();
    }

    public List<Tarif> listerActifs() {
        return tarifRepository.findByActifTrue();
    }

    public Tarif consulter(Long id) {
        return tarifRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Tarif introuvable avec l'id : " + id));
    }

    public Tarif modifier(Long id, Tarif tarif) {
        Tarif existant = consulter(id);
        valider(tarif);
        existant.setNom(tarif.getNom());
        existant.setTypevetement(tarif.getTypevetement());
        existant.setTypeNettoyage(tarif.getTypeNettoyage());
        existant.setPrixauklo(tarif.getPrixauklo());
        existant.setPrixunitaire(tarif.getPrixunitaire());
        return tarifRepository.save(existant);
    }

    public void desactiver(Long id) {
        Tarif tarif = consulter(id);
        tarif.setActif(false);
        tarifRepository.save(tarif);
    }

    public Tarif activer(Long id) {
        Tarif tarif = consulter(id);
        tarif.setActif(true);
        return tarifRepository.save(tarif);
    }

    private void valider(Tarif tarif) {
        if (tarif.getNom() == null || tarif.getNom().isBlank()) {
            throw new IllegalArgumentException("Le nom du tarif est obligatoire");
        }
        if (encoreNul(tarif.getPrixauklo()) && encoreNul(tarif.getPrixunitaire())) {
            throw new IllegalArgumentException("Renseignez au moins un prix (au kilo ou à la pièce)");
        }
        if (tarif.getPrixauklo() != null && tarif.getPrixauklo().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le prix au kilo ne peut pas être négatif");
        }
        if (tarif.getPrixunitaire() != null && tarif.getPrixunitaire().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le prix unitaire ne peut pas être négatif");
        }
    }

    private boolean encoreNul(BigDecimal valeur) {
        return valeur == null || valeur.compareTo(BigDecimal.ZERO) == 0;
    }
}