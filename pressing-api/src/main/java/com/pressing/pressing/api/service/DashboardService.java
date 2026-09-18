package com.pressing.pressing.api.service;

import com.pressing.pressing.api.dto.response.AlerteClientImpayeDTO;
import com.pressing.pressing.api.dto.response.AlerteCommandeRetardDTO;
import com.pressing.pressing.api.dto.response.AlerteStockCritiqueDTO;
import com.pressing.pressing.api.dto.response.DashboardResumeDTO;
import com.pressing.pressing.api.dto.response.DashboardTendancesDTO;
import com.pressing.pressing.api.dto.response.TendanceDTO;
import com.pressing.pressing.api.entite.Client;
import com.pressing.pressing.api.entite.Commande;
import com.pressing.pressing.api.entite.StatutCommande;
import com.pressing.pressing.api.entite.Vetements;
import com.pressing.pressing.api.repository.CommandeRepository;
import com.pressing.pressing.api.repository.PaiementRepository;
import com.pressing.pressing.api.repository.VetementsRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int SEUIL_STOCK_CRITIQUE = 5;

    private final CommandeRepository commandeRepository;
    private final PaiementRepository paiementRepository;
    private final VetementsRepository vetementsRepository;

    @Transactional(readOnly = true)
    public DashboardResumeDTO getResume() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutSemaine = aujourdhui.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate debutMois = aujourdhui.withDayOfMonth(1);

        DashboardResumeDTO resume = new DashboardResumeDTO();
        resume.setTendances(calculerTendances(aujourdhui, debutSemaine, debutMois));
        resume.setCommandesEnRetard(calculerCommandesEnRetard());
        resume.setClientsImpayes(calculerClientsImpayes());
        resume.setStockCritique(calculerStockCritique());
        return resume;
    }

    private DashboardTendancesDTO calculerTendances(LocalDate aujourdhui, LocalDate debutSemaine, LocalDate debutMois) {
        LocalDate hier = aujourdhui.minusDays(1);
        LocalDate semainePrecedenteFin = debutSemaine.minusDays(1);
        LocalDate semainePrecedenteDebut = debutSemaine.minusWeeks(1);
        LocalDate moisPrecedentFin = debutMois.minusDays(1);
        LocalDate moisPrecedentDebut = debutMois.minusMonths(1);

        TendanceDTO caJour = new TendanceDTO(
                nonNull(paiementRepository.sumMontantByDate(aujourdhui)),
                nonNull(paiementRepository.sumMontantByDate(hier)));
        TendanceDTO caSemaine = new TendanceDTO(
                nonNull(paiementRepository.sumMontantBetween(debutSemaine.atStartOfDay(), finDeJour(aujourdhui))),
                nonNull(paiementRepository.sumMontantBetween(semainePrecedenteDebut.atStartOfDay(), finDeJour(semainePrecedenteFin))));
        TendanceDTO caMois = new TendanceDTO(
                nonNull(paiementRepository.sumMontantBetween(debutMois.atStartOfDay(), finDeJour(aujourdhui))),
                nonNull(paiementRepository.sumMontantBetween(moisPrecedentDebut.atStartOfDay(), finDeJour(moisPrecedentFin))));

        long commandesEnCours = 0;
        long commandesTerminees = 0;
        for (StatutCommande statut : StatutCommande.values()) {
            long nb = commandeRepository.countByStatut(statut);
            if (estEnCours(statut)) {
                commandesEnCours += nb;
            } else if (estTerminee(statut)) {
                commandesTerminees += nb;
            }
        }

        long totalCommandes = commandeRepository.count();
        BigDecimal tauxTransformation = totalCommandes == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(commandesTerminees)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalCommandes), 1, java.math.RoundingMode.HALF_UP);

        DashboardTendancesDTO tendances = new DashboardTendancesDTO();
        tendances.setCaJour(caJour);
        tendances.setCaSemaine(caSemaine);
        tendances.setCaMois(caMois);
        tendances.setCommandesEnCours(commandesEnCours);
        tendances.setCommandesTerminees(commandesTerminees);
        tendances.setTauxTransformation(tauxTransformation);
        return tendances;
    }

    private List<AlerteCommandeRetardDTO> calculerCommandesEnRetard() {
        List<StatutCommande> terminees = List.of(
                StatutCommande.RECUPERE,
                StatutCommande.LIVRE,
                StatutCommande.PAYEE);
        return commandeRepository.findEnRetard(LocalDateTime.now(), terminees).stream()
                .limit(20)
                .map(c -> {
                    AlerteCommandeRetardDTO dto = new AlerteCommandeRetardDTO();
                    dto.setCommandeId(c.getIdcommande());
                    dto.setNumeroTicket(c.getNumeroTicket());
                    if (c.getClient() != null) {
                        dto.setClientNom(c.getClient().getNom());
                        dto.setClientTelephone(c.getClient().getTelephone());
                    }
                    dto.setDateRecuperationPrevue(c.getDateRecuperationPrevue() != null
                            ? c.getDateRecuperationPrevue().toLocalDate() : null);
                    dto.setJoursRetard(c.getDateRecuperationPrevue() != null
                            ? java.time.Duration.between(c.getDateRecuperationPrevue(), LocalDateTime.now()).toDays() : 0);
                    return dto;
                })
                .toList();
    }

    private List<AlerteClientImpayeDTO> calculerClientsImpayes() {
        Map<Long, AlerteClientImpayeDTO> parClient = new LinkedHashMap<>();
        for (Commande c : commandeRepository.findByMontantPayeLessThanMontantTotal()) {
            if (c.getClient() == null) {
                continue;
            }
            Client client = c.getClient();
            AlerteClientImpayeDTO dto = parClient.computeIfAbsent(client.getIdclient(), k -> {
                AlerteClientImpayeDTO n = new AlerteClientImpayeDTO();
                n.setClientId(client.getIdclient());
                n.setNom(client.getNom());
                n.setTelephone(client.getTelephone());
                n.setMontantImpaye(BigDecimal.ZERO);
                n.setNbCommandesImpayees(0);
                return n;
            });
            dto.setMontantImpaye(dto.getMontantImpaye().add(c.getResteAPayer()));
            dto.setNbCommandesImpayees(dto.getNbCommandesImpayees() + 1);
        }
        return parClient.values().stream()
                .sorted(Comparator.comparing(AlerteClientImpayeDTO::getMontantImpaye).reversed())
                .limit(10)
                .toList();
    }

    private List<AlerteStockCritiqueDTO> calculerStockCritique() {
        return vetementsRepository.findByQuantiteStockLessThanEqual(SEUIL_STOCK_CRITIQUE).stream()
                .sorted(Comparator.comparing(Vetements::getQuantiteStock))
                .map(v -> {
                    AlerteStockCritiqueDTO dto = new AlerteStockCritiqueDTO();
                    dto.setVetementId(v.getIdvetement());
                    dto.setLibelle(v.getLibelle());
                    dto.setTypedevetement(v.getTypedevetement());
                    dto.setQuantiteStock(v.getQuantiteStock());
                    dto.setSeuil(SEUIL_STOCK_CRITIQUE);
                    return dto;
                })
                .toList();
    }

    private boolean estEnCours(StatutCommande statut) {
        return statut == StatutCommande.RECU
                || statut == StatutCommande.INVENTAIRE
                || statut == StatutCommande.EN_LAVAGE
                || statut == StatutCommande.REPASSAGE
                || statut == StatutCommande.PRET;
    }

    private boolean estTerminee(StatutCommande statut) {
        return statut == StatutCommande.RECUPERE
                || statut == StatutCommande.LIVRE
                || statut == StatutCommande.PAYEE;
    }

    private LocalDateTime finDeJour(LocalDate date) {
        return date.atTime(23, 59, 59);
    }

    private BigDecimal nonNull(BigDecimal valeur) {
        return valeur != null ? valeur : BigDecimal.ZERO;
    }
}