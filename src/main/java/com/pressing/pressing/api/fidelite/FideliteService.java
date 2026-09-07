package com.pressing.pressing.api.fidelite;

import com.pressing.pressing.api.client.Client;
import com.pressing.pressing.api.client.ClientRepository;
import com.pressing.pressing.api.commande.Commande;
import com.pressing.pressing.api.commande.CommandeRepository;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FideliteService {

    private final HistoriquePointRepository historiquePointRepository;
    private final ClientRepository clientRepository;
    private final CommandeRepository commandeRepository;

    private static final int VALEUR_POINT = 1;

    @Transactional(readOnly = true)
    public int soldePoints(Long clientId) {
        Client client = getClient(clientId);
        return client.getPoints_fidelites();
    }

    @Transactional(readOnly = true)
    public List<HistoriquePoint> historiquePoints(Long clientId) {
        getClient(clientId);
        return historiquePointRepository.findByClientIdOrderByDateDesc(clientId);
    }

    @Transactional
    public void crediterPoints(Long clientId, Long commandeId, BigDecimal montantPaye) {
        if (montantPaye == null || montantPaye.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        int nbPoints = montantPaye.divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();
        if (nbPoints <= 0) {
            return;
        }
        Client client = getClient(clientId);
        client.setPoints_fidelites(client.getPoints_fidelites() + nbPoints);
        clientRepository.save(client);

        historiquePointRepository.save(HistoriquePoint.builder()
                .clientId(clientId)
                .commandeId(commandeId)
                .points(nbPoints)
                .type("CREDIT")
                .description("Paiement " + montantPaye + " FCFA")
                .build());
    }

    @Transactional
    public BigDecimal utiliserPoints(Long clientId, Long commandeId, int nbPoints) {
        Client client = getClient(clientId);
        if (client.getPoints_fidelites() < nbPoints) {
            throw new IllegalArgumentException("Solde insuffisant : vous avez " + client.getPoints_fidelites() + " points, " + nbPoints + " demandés");
        }
        Commande commande = getCommande(commandeId);
        BigDecimal reste = commande.getMontantTotal()
                .subtract(commande.getMontantPaye())
                .subtract(commande.getRemise() != null ? commande.getRemise() : BigDecimal.ZERO);
        BigDecimal remise = BigDecimal.valueOf(nbPoints * VALEUR_POINT);
        if (remise.compareTo(reste) > 0) {
            throw new IllegalArgumentException("Remise de " + remise + " FCFA dépasse le reste à payer (" + reste + " FCFA)");
        }

        client.setPoints_fidelites(client.getPoints_fidelites() - nbPoints);
        clientRepository.save(client);

        commande.setRemise(commande.getRemise() != null
                ? commande.getRemise().add(remise)
                : remise);
        commandeRepository.save(commande);

        historiquePointRepository.save(HistoriquePoint.builder()
                .clientId(clientId)
                .commandeId(commandeId)
                .points(-nbPoints)
                .type("UTILISATION")
                .description("Remise de " + remise + " FCFA sur commande " + commande.getNumeroTicket())
                .build());

        return remise;
    }

    @Transactional
    public void crediterPointsManuellement(Long clientId, Integer nbPoints, String description) {
        if (nbPoints <= 0) {
            throw new IllegalArgumentException("Le nombre de points doit être supérieur à zéro");
        }
        Client client = getClient(clientId);
        client.setPoints_fidelites(client.getPoints_fidelites() + nbPoints);
        clientRepository.save(client);

        historiquePointRepository.save(HistoriquePoint.builder()
                .clientId(clientId)
                .points(nbPoints)
                .type("CREDIT_MANUEL")
                .description(description != null ? description : "Crédit manuel")
                .build());
    }

    private Client getClient(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Client introuvable (id " + id + ")"));
    }

    private Commande getCommande(Long id) {
        return commandeRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable (id " + id + ")"));
    }
}