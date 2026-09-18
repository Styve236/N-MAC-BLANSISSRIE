package com.pressing.pressing.api.service;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import com.pressing.pressing.api.dto.request.PaiementDTO;
import com.pressing.pressing.api.dto.response.PaiementResponseDTO;
import com.pressing.pressing.api.dto.response.StatutPaiementDTO;
import com.pressing.pressing.api.entite.Client;
import com.pressing.pressing.api.entite.Commande;
import com.pressing.pressing.api.entite.Paiement;
import com.pressing.pressing.api.entite.TypePaiement;
import com.pressing.pressing.api.repository.ClientRepository;
import com.pressing.pressing.api.repository.CommandeRepository;
import com.pressing.pressing.api.repository.PaiementRepository;
import com.pressing.pressing.api.service.FideliteService;
import com.pressing.pressing.api.service.NotificationSmsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;




@Service
@RequiredArgsConstructor
public class PaiementService {
    private final PaiementRepository paiementRepository;
    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final NotificationSmsService notificationSmsService;
    private final FideliteService fideliteService;

    public Page<PaiementResponseDTO> listerTous(Long clientId, Pageable pageable) {
        Page<Paiement> page;
        if (clientId != null) {
            page = paiementRepository.findByClientIdclient(clientId, pageable);
        } else {
            page = paiementRepository.findAllByOrderByDatePaiementDesc(pageable);
        }
        return page.map(this::toDTO);
    }

    public BigDecimal getCAduJour(LocalDate date){
        return paiementRepository.sumMontantByDate(date);
    }

    @Transactional
    public PaiementResponseDTO ajouter(Long commandeId, PaiementDTO dto) {
        if (dto.getMontant() == null || dto.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit être supérieur à zéro");
        }
        if (dto.getMoyenPaiement() == null) {
            throw new IllegalArgumentException("Le moyen de paiement est obligatoire");
        }

        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable avec l'id : " + commandeId));

        BigDecimal reste = commande.getResteAPayer();
        if (dto.getMontant().compareTo(reste) > 0) {
            throw new IllegalArgumentException("Le montant dépasse le reste à payer (" + reste + " FCFA)");
        }

        // Classification automatique du paiement : ACOMPTE / SOLDE / INTEGRAL
        BigDecimal avant = commande.getMontantPaye() != null ? commande.getMontantPaye() : BigDecimal.ZERO;
        BigDecimal apres = avant.add(dto.getMontant());
        BigDecimal montantAFerme = commande.getMontantTotal() != null ? commande.getMontantTotal() : BigDecimal.ZERO;
        BigDecimal remise = commande.getRemise() != null ? commande.getRemise() : BigDecimal.ZERO;
        BigDecimal aPayer = montantAFerme.subtract(remise);
        TypePaiement typePaiement;
        if (avant.compareTo(BigDecimal.ZERO) == 0 && apres.compareTo(aPayer) >= 0) {
            typePaiement = TypePaiement.INTEGRAL;
        } else if (apres.compareTo(aPayer) >= 0) {
            typePaiement = TypePaiement.SOLDE;
        } else {
            typePaiement = TypePaiement.ACOMPTE;
        }

        Paiement paiement = Paiement.builder()
                .montant(dto.getMontant())
                .moyenPaiement(dto.getMoyenPaiement())
                .typePaiement(typePaiement)
                .referenceTransaction(dto.getReferenceTransaction())
                .datePaiement(LocalDateTime.now())
                .build();

        commande.addpaiement(paiement);
        Commande commandeMaj = commandeRepository.save(commande);

        notificationSmsService.envoyerPaiementRecu(commandeMaj, paiement.getMontant());

        fideliteService.crediterPoints(
                commande.getClient().getIdclient(),
                commande.getIdcommande(),
                paiement.getMontant());

        return toDTO(paiement);
    }

    @Transactional(readOnly = true)
    public List<PaiementResponseDTO> listerParCommande(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable avec l'id : " + commandeId));

        return commande.getPaiements().stream()
                .sorted(Comparator.comparing(Paiement::getDatePaiement).reversed())
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaiementResponseDTO> listerParClient(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new RessourceNotFoundException("Client introuvable avec l'id : " + clientId);
        }

        return commandeRepository.findByClientIdclient(clientId).stream()
                .flatMap(c -> c.getPaiements().stream())
                .sorted(Comparator.comparing(Paiement::getDatePaiement).reversed())
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public StatutPaiementDTO statutPaiement(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable avec l'id : " + commandeId));

        BigDecimal remise = commande.getRemise() != null ? commande.getRemise() : BigDecimal.ZERO;
        StatutPaiementDTO dto = new StatutPaiementDTO();
        dto.setIdcommande(commande.getIdcommande());
        dto.setNumeroTicket(commande.getNumeroTicket());
        dto.setMontantTotal(commande.getMontantTotal());
        dto.setMontantPaye(commande.getMontantPaye());
        dto.setResteAPayer(commande.getResteAPayer());
        dto.setPayee(commande.isPayee());
        dto.setPaiements(commande.getPaiements().stream()
                .sorted(Comparator.comparing(Paiement::getDatePaiement))
                .map(this::toDTO)
                .toList());
        return dto;
    }

    private PaiementResponseDTO toDTO(Paiement paiement) {
        PaiementResponseDTO dto = new PaiementResponseDTO();
        dto.setIdpaiement(paiement.getIdpaiement());
        dto.setMontant(paiement.getMontant());
        dto.setMoyenPaiement(paiement.getMoyenPaiement() != null ? paiement.getMoyenPaiement().name() : null);
        dto.setTypePaiement(paiement.getTypePaiement() != null ? paiement.getTypePaiement().name() : null);
        dto.setReferenceTransaction(paiement.getReferenceTransaction());
        dto.setDatePaiement(paiement.getDatePaiement());
        if (paiement.getCommande() != null) {
            dto.setIdcommande(paiement.getCommande().getIdcommande());
            dto.setNumeroTicket(paiement.getCommande().getNumeroTicket());
        }
        return dto;
    }
}