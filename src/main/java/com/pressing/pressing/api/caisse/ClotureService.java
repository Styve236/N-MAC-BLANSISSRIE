package com.pressing.pressing.api.caisse;

import com.pressing.pressing.api.Paiement.MoyenPaiement;
import com.pressing.pressing.api.Paiement.Paiement;
import com.pressing.pressing.api.Paiement.PaiementRepository;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClotureService {

    private final PaiementRepository paiementRepository;
    private final ClotureCaisseRepository clotureCaisseRepository;

    @Transactional(readOnly = true)
    public ClotureDTO rapport(LocalDate date) {
        LocalDate jour = date != null ? date : LocalDate.now();
        List<Paiement> paiements = paiementRepository
                .findByDatePaiementBetween(jour.atStartOfDay(), jour.atTime(23, 59, 59));
        ClotureDTO dto = calculer(jour, paiements);
        clotureCaisseRepository.findTopByDateOrderByDateClotureDesc(jour)
                .ifPresent(c -> {
                    dto.setCloturee(true);
                    dto.setIdcloture(c.getIdcloture());
                    dto.setDateCloture(c.getDateCloture());
                    dto.setTotalCompte(c.getTotalCompte());
                    dto.setEcart(c.getEcart());
                    dto.setObservations(c.getObservations());
                });
        return dto;
    }

    @Transactional
    public ClotureDTO cloturer(ClotureRequestDTO requete) {
        if (requete.getMontantEnCaisse() == null) {
            throw new IllegalArgumentException("Le montant compté en caisse est obligatoire");
        }
        LocalDate jour = requete.getDate() != null ? requete.getDate() : LocalDate.now();
        List<Paiement> paiements = paiementRepository
                .findByDatePaiementBetween(jour.atStartOfDay(), jour.atTime(23, 59, 59));
        ClotureDTO rapport = calculer(jour, paiements);

        BigDecimal especes = montantEspèces(rapport);
        BigDecimal ecart = requete.getMontantEnCaisse().subtract(especes);

        ClotureCaisse cloture = ClotureCaisse.builder()
                .date(jour)
                .dateCloture(LocalDateTime.now())
                .totalLogiciel(rapport.getTotalLogiciel())
                .totalCompte(requete.getMontantEnCaisse())
                .ecart(ecart)
                .nombrePaiements(rapport.getNombrePaiements())
                .detailParMoyen(serialiser(rapport.getParMoyen()))
                .observations(requete.getObservations())
                .build();
        clotureCaisseRepository.save(cloture);
        return toDTO(cloture);
    }

    @Transactional(readOnly = true)
    public List<ClotureDTO> historique() {
        return clotureCaisseRepository.findAllByOrderByDateClotureDesc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClotureDTO dernierParDate(LocalDate date) {
        return clotureCaisseRepository.findTopByDateOrderByDateClotureDesc(date)
                .map(this::toDTO)
                .orElseThrow(() -> new RessourceNotFoundException("Aucune clôture enregistrée pour le " + date));
    }

    private ClotureDTO calculer(LocalDate jour, List<Paiement> paiements) {
        BigDecimal total = paiements.stream()
                .map(Paiement::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<MoyenPaiement, List<Paiement>> groupe = paiements.stream()
                .collect(Collectors.groupingBy(Paiement::getMoyenPaiement));

        List<MontantParMoyenDTO> parMoyen = new ArrayList<>();
        for (Map.Entry<MoyenPaiement, List<Paiement>> e : groupe.entrySet()) {
            MontantParMoyenDTO dto = new MontantParMoyenDTO();
            dto.setMoyen(e.getKey().name());
            dto.setMontant(e.getValue().stream()
                    .map(Paiement::getMontant)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            dto.setNombre(e.getValue().size());
            parMoyen.add(dto);
        }
        parMoyen.sort(Comparator.comparing(MontantParMoyenDTO::getMoyen));

        ClotureDTO dto = new ClotureDTO();
        dto.setDate(jour);
        dto.setTotalLogiciel(total);
        dto.setNombrePaiements(paiements.size());
        dto.setParMoyen(parMoyen);
        dto.setCloturee(false);
        return dto;
    }

    private BigDecimal montantEspèces(ClotureDTO rapport) {
        return rapport.getParMoyen().stream()
                .filter(m -> "ESPECES".equals(m.getMoyen()))
                .map(MontantParMoyenDTO::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ClotureDTO toDTO(ClotureCaisse c) {
        ClotureDTO dto = new ClotureDTO();
        dto.setIdcloture(c.getIdcloture());
        dto.setDate(c.getDate());
        dto.setDateCloture(c.getDateCloture());
        dto.setTotalLogiciel(c.getTotalLogiciel());
        dto.setTotalCompte(c.getTotalCompte());
        dto.setEcart(c.getEcart());
        dto.setNombrePaiements(c.getNombrePaiements());
        dto.setParMoyen(deserialiser(c.getDetailParMoyen()));
        dto.setObservations(c.getObservations());
        dto.setCloturee(true);
        return dto;
    }

    private String serialiser(List<MontantParMoyenDTO> liste) {
        if (liste == null || liste.isEmpty()) {
            return "";
        }
        return liste.stream()
                .map(m -> m.getMoyen() + ":" + m.getMontant() + ":" + m.getNombre())
                .collect(Collectors.joining(";"));
    }

    private List<MontantParMoyenDTO> deserialiser(String detail) {
        List<MontantParMoyenDTO> liste = new ArrayList<>();
        if (detail == null || detail.isBlank()) {
            return liste;
        }
        for (String partie : detail.split(";")) {
            String[] p = partie.split(":");
            if (p.length < 3) continue;
            MontantParMoyenDTO dto = new MontantParMoyenDTO();
            dto.setMoyen(p[0]);
            try {
                dto.setMontant(new BigDecimal(p[1]));
            } catch (NumberFormatException e) {
                dto.setMontant(BigDecimal.ZERO);
            }
            dto.setNombre(Integer.parseInt(p[2]));
            liste.add(dto);
        }
        return liste;
    }
}