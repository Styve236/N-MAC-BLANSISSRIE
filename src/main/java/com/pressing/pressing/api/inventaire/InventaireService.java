package com.pressing.pressing.api.inventaire;

import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventaireService {

    private final VetementsRepository vetementsRepository;
    private final InventaireRepository inventaireRepository;

    // ---------- Catalogue des vêtements ----------

    @Transactional(readOnly = true)
    public List<VetementDTO> listeVetements() {
        return vetementsRepository.findAllByOrderByLibelleAsc().stream()
                .map(this::toVetementDTO)
                .toList();
    }

    @Transactional
    public VetementDTO creerVetement(VetementDTO dto) {
        if (dto.getLibelle() == null || dto.getLibelle().isBlank()) {
            throw new IllegalArgumentException("Le libellé du vêtement est obligatoire");
        }
        Vetements v = new Vetements();
        v.setLibelle(dto.getLibelle().trim());
        v.setTypedevetement(dto.getTypedevetement());
        v.setQuantiteStock(dto.getQuantiteStock() == null ? 0 : dto.getQuantiteStock());
        vetementsRepository.save(v);
        return toVetementDTO(v);
    }

    @Transactional
    public VetementDTO modifierVetement(Long id, VetementDTO dto) {
        Vetements v = getVetement(id);
        if (dto.getLibelle() != null && !dto.getLibelle().isBlank()) {
            v.setLibelle(dto.getLibelle().trim());
        }
        v.setTypedevetement(dto.getTypedevetement());
        if (dto.getQuantiteStock() != null && dto.getQuantiteStock() >= 0) {
            v.setQuantiteStock(dto.getQuantiteStock());
        }
        vetementsRepository.save(v);
        return toVetementDTO(v);
    }

    @Transactional
    public void supprimerVetement(Long id) {
        Vetements v = getVetement(id);
        vetementsRepository.delete(v);
    }

    // ---------- Bons d'inventaire (réceptions / sorties) ----------

    @Transactional(readOnly = true)
    public List<BonInventaireDTO> listeBons() {
        return inventaireRepository.findAllByOrderByDateInventaireDesc().stream()
                .map(this::toBonDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public BonInventaireDTO getBon(Long id) {
        Inventaire bon = inventaireRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Bon d'inventaire introuvable (id " + id + ")"));
        return toBonDTO(bon);
    }

    @Transactional
    public BonInventaireDTO creerBon(BonInventaireRequestDTO requete) {
        if (requete.getLignes() == null || requete.getLignes().isEmpty()) {
            throw new IllegalArgumentException("Le bon doit contenir au moins une ligne");
        }
        LocalDate date = requete.getDate() != null ? requete.getDate() : LocalDate.now();

        Inventaire bon = Inventaire.builder()
                .dateInventaire(date)
                .observations(requete.getObservations())
                .build();

        for (LigneInventaireRequestDTO l : requete.getLignes()) {
            if (l.getVetementId() == null) {
                throw new IllegalArgumentException("Chaque ligne doit référencer un vêtement");
            }
            if (l.getQuantite() == null || l.getQuantite() == 0) {
                throw new IllegalArgumentException("La quantité d'une ligne doit être différente de zéro");
            }
            Vetements v = getVetement(l.getVetementId());
            int nouveauStock = v.getQuantiteStock() + l.getQuantite();
            if (nouveauStock < 0) {
                throw new IllegalArgumentException("Stock insuffisant pour « " + v.getLibelle() + " » : sortie de " + l.getQuantite()
                        + " alors que le stock est de " + v.getQuantiteStock());
            }
            v.setQuantiteStock(nouveauStock);
            vetementsRepository.save(v);

            LigneInventaire ligne = LigneInventaire.builder()
                    .vetements(v)
                    .inventaire(bon)
                    .quantite(l.getQuantite())
                    .dateEntree(date)
                    .etat(l.getEtat() == null || l.getEtat().isBlank() ? "BON" : l.getEtat().trim())
                    .build();
            bon.getLignes().add(ligne);
        }

        inventaireRepository.save(bon);
        return toBonDTO(bon);
    }

    private Vetements getVetement(Long id) {
        return vetementsRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Vêtement introuvable (id " + id + ")"));
    }

    private VetementDTO toVetementDTO(Vetements v) {
        VetementDTO dto = new VetementDTO();
        dto.setIdvetement(v.getIdvetement());
        dto.setLibelle(v.getLibelle());
        dto.setTypedevetement(v.getTypedevetement());
        dto.setQuantiteStock(v.getQuantiteStock());
        return dto;
    }

    private BonInventaireDTO toBonDTO(Inventaire bon) {
        BonInventaireDTO dto = new BonInventaireDTO();
        dto.setIdinventaire(bon.getIdinventaire());
        dto.setDateInventaire(bon.getDateInventaire());
        dto.setObservations(bon.getObservations());
        int total = 0;
        for (LigneInventaire l : bon.getLignes()) {
            total += (l.getQuantite() == null ? 0 : l.getQuantite());
            LigneInventaireDTO ldto = new LigneInventaireDTO();
            ldto.setIdligneinventaire(l.getIdligneinventaire());
            ldto.setVetementId(l.getVetements().getIdvetement());
            ldto.setVetementLibelle(l.getVetements().getLibelle());
            ldto.setQuantite(l.getQuantite());
            ldto.setDateEntree(l.getDateEntree());
            ldto.setEtat(l.getEtat());
            dto.getLignes().add(ldto);
        }
        dto.setTotalQuantite(total);
        return dto;
    }
}