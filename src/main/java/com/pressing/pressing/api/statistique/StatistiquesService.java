package com.pressing.pressing.api.statistique;

import com.pressing.pressing.api.Paiement.MoyenPaiement;
import com.pressing.pressing.api.Paiement.Paiement;
import com.pressing.pressing.api.Paiement.PaiementRepository;
import com.pressing.pressing.api.client.ClientRepository;
import com.pressing.pressing.api.commande.Commande;
import com.pressing.pressing.api.commande.CommandeRepository;
import com.pressing.pressing.api.commande.LigneCommande;
import com.pressing.pressing.api.commande.StatutCommande;
import com.pressing.pressing.api.commande.Tarif;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatistiquesService {

    private final CommandeRepository commandeRepository;
    private final PaiementRepository paiementRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public StatistiquesDTO getStatistiques() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDateTime debutJour = aujourdhui.atStartOfDay();
        LocalDateTime debutMois = aujourdhui.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finJour = aujourdhui.atTime(23, 59, 59);

        List<Commande> commandes = commandeRepository.findAll();
        List<Paiement> paiements = paiementRepository.findAll();

        StatistiquesDTO stats = new StatistiquesDTO();
        stats.setChiffreAffairesJour(nonNull(paiementRepository.sumMontantByDate(aujourdhui)));
        stats.setChiffreAffairesMois(nonNull(paiementRepository.sumMontantBetween(debutMois, finJour)));
        stats.setTotalEncaisse(paiements.stream()
                .map(Paiement::getMontant)
                .filter(m -> m != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        stats.setTotalCommandes(commandes.size());
        stats.setCommandesAujourdhui(commandes.stream()
                .filter(c -> c.getDateCreation() != null && c.getDateCreation().isAfter(debutJour))
                .count());
        stats.setTotalClients(clientRepository.count());

        List<Commande> impayees = commandes.stream().filter(Commande::aUnsolde).toList();
        stats.setCommandesImpayees(impayees.size());
        stats.setMontantImpayeTotal(impayees.stream()
                .map(c -> c.getMontantTotal().subtract(c.getMontantPaye()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        Map<StatutCommande, Long> parStatut = new EnumMap<>(StatutCommande.class);
        for (StatutCommande statut : StatutCommande.values()) {
            parStatut.put(statut, 0L);
        }
        parStatut.putAll(commandes.stream()
                .filter(c -> c.getStatut() != null)
                .collect(Collectors.groupingBy(Commande::getStatut, Collectors.counting())));
        stats.setCommandesParStatut(parStatut);

        Map<MoyenPaiement, BigDecimal> parMoyen = new EnumMap<>(MoyenPaiement.class);
        paiements.stream()
                .filter(p -> p.getMoyenPaiement() != null && p.getMontant() != null)
                .collect(Collectors.groupingBy(Paiement::getMoyenPaiement,
                        Collectors.mapping(Paiement::getMontant, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                .forEach(parMoyen::put);
        stats.setEncaissementParMoyen(orderMoyens(parMoyen));

        stats.setTopClients(calculerTopClients(commandes));
        stats.setTopPrestations(calculerTopPrestations(commandes));
        return stats;
    }

    private List<TopClientDTO> calculerTopClients(List<Commande> commandes) {
        Map<Long, TopClientDTO> parClient = new HashMap<>();
        for (Commande c : commandes) {
            if (c.getClient() == null) {
                continue;
            }
            TopClientDTO top = parClient.computeIfAbsent(c.getClient().getIdclient(), k -> {
                TopClientDTO dto = new TopClientDTO();
                dto.setNom(c.getClient().getNom());
                dto.setTelephone(c.getClient().getTelephone());
                dto.setNbCommandes(0);
                dto.setTotalDepense(BigDecimal.ZERO);
                return dto;
            });
            top.setNbCommandes(top.getNbCommandes() + 1);
            top.setTotalDepense(top.getTotalDepense().add(c.getMontantTotal()));
        }
        return parClient.values().stream()
                .sorted(Comparator.comparing(TopClientDTO::getTotalDepense).reversed())
                .limit(5)
                .toList();
    }

    private List<TopPrestationDTO> calculerTopPrestations(List<Commande> commandes) {
        Map<String, TopPrestationDTO> parTarif = new LinkedHashMap<>();
        for (Commande c : commandes) {
            for (LigneCommande ligne : c.getLignes()) {
                Tarif tarif = ligne.getTarif();
                String cle = tarif != null && tarif.getIdtarif() != null
                        ? tarif.getIdtarif().toString()
                        : String.valueOf(System.identityHashCode(ligne));
                TopPrestationDTO top = parTarif.computeIfAbsent(cle, k -> {
                    TopPrestationDTO dto = new TopPrestationDTO();
                    if (tarif != null) {
                        dto.setNomTarif(tarif.getNom() != null ? tarif.getNom() : tarif.getTypevetement());
                        dto.setTypevetement(tarif.getTypevetement());
                        dto.setTypeNettoyage(tarif.getTypeNettoyage());
                    }
                    dto.setNbPrestations(0);
                    dto.setTotal(BigDecimal.ZERO);
                    return dto;
                });
                top.setNbPrestations(top.getNbPrestations() + 1);
                if (ligne.getMontant() != null) {
                    top.setTotal(top.getTotal().add(ligne.getMontant()));
                }
            }
        }
        return parTarif.values().stream()
                .sorted(Comparator.comparing(TopPrestationDTO::getTotal).reversed())
                .limit(5)
                .toList();
    }

    private Map<MoyenPaiement, BigDecimal> orderMoyens(Map<MoyenPaiement, BigDecimal> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<MoyenPaiement, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private BigDecimal nonNull(BigDecimal valeur) {
        return valeur != null ? valeur : BigDecimal.ZERO;
    }
}