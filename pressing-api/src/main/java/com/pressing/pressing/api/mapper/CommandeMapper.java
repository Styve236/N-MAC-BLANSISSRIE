package com.pressing.pressing.api.mapper;

import com.pressing.pressing.api.dto.response.ClientResumeDTO;
import com.pressing.pressing.api.dto.response.CommandeDetailDTO;
import com.pressing.pressing.api.dto.response.LigneCommandeDetailDTO;
import com.pressing.pressing.api.dto.response.PaiementResumeDTO;
import com.pressing.pressing.api.dto.response.PhotoDTO;
import com.pressing.pressing.api.dto.response.TarifResumeDTO;
import com.pressing.pressing.api.entite.Client;
import com.pressing.pressing.api.entite.Commande;
import com.pressing.pressing.api.entite.LigneCommande;
import com.pressing.pressing.api.entite.Paiement;
import com.pressing.pressing.api.entite.Photo;
import com.pressing.pressing.api.entite.Tarif;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Conversion entite JPA -> DTO de reponse.
 * Doit etre appele dans une transaction : les collections (lignes, photos, paiements)
 * sont lazy et ne se remplissent qu'a l'interieur de la session.
 */
@Component
public class CommandeMapper {

    public CommandeDetailDTO toDTO(Commande commande) {
        if (commande == null) {
            return null;
        }
        CommandeDetailDTO dto = new CommandeDetailDTO();
        dto.setIdcommande(commande.getIdcommande());
        dto.setNumeroTicket(commande.getNumeroTicket());
        dto.setDateCreation(commande.getDateCreation());
        dto.setDateRecuperationPrevue(commande.getDateRecuperationPrevue());
        dto.setDateRetraitReelle(commande.getDateRetraitReelle());
        dto.setStatut(commande.getStatut());
        dto.setPoidsTotal(commande.getPoidsTotal());
        dto.setMontantTotal(commande.getMontantTotal());
        dto.setMontantPaye(commande.getMontantPaye());
        dto.setRemise(commande.getRemise());
        // Champs calcules exposes par l'entite : calcules ici car ils utilsent
        // les paiements (le frontend en a besoin pour le badge "payee" et le reste).
        dto.setResteAPayer(commande.getResteAPayer());
        dto.setPayee(commande.isPayee());
        dto.setAdresseLivraison(commande.getAdresseLivraison());
        dto.setFraisLivraison(commande.getFraisLivraison());
        dto.setDateLivraisonPrevue(commande.getDateLivraisonPrevue());
        dto.setDateLivraisonReelle(commande.getDateLivraisonReelle());
        dto.setStatutLivraison(commande.getStatutLivraison());
        dto.setMasqueeParLivreur(commande.isMasqueeParLivreur());
        dto.setClient(toClientResume(commande.getClient()));

        List<LigneCommandeDetailDTO> lignes = dto.getLignes();
        lignes.addAll(commande.getLignes().stream().map(this::toLigneDTO).toList());
        List<PaiementResumeDTO> paiements = dto.getPaiements();
        paiements.addAll(commande.getPaiements().stream().map(this::toPaiementDTO).toList());
        return dto;
    }

    private ClientResumeDTO toClientResume(Client client) {
        if (client == null) {
            return null;
        }
        ClientResumeDTO dto = new ClientResumeDTO();
        dto.setIdclient(client.getIdclient());
        dto.setNom(client.getNom());
        dto.setTelephone(client.getTelephone());
        dto.setVille(client.getVille());
        dto.setQuartier(client.getQuartier());
        dto.setPoints_fidelites(client.getPoints_fidelites());
        return dto;
    }

    private LigneCommandeDetailDTO toLigneDTO(LigneCommande ligne) {
        LigneCommandeDetailDTO dto = new LigneCommandeDetailDTO();
        dto.setIdligne(ligne.getIdligne());
        dto.setPoids(ligne.getPoids());
        dto.setQuantite(ligne.getQuantite());
        dto.setMontant(ligne.getMontant());
        dto.setDescription(ligne.getDescription());
        dto.setTypeNettoyage(ligne.getTypeNettoyage());
        dto.setTarif(toTarifResume(ligne.getTarif()));
        for (Photo photo : ligne.getPhotos()) {
            dto.getPhotos().add(toPhotoDTO(photo));
        }
        return dto;
    }

    private PhotoDTO toPhotoDTO(Photo photo) {
        PhotoDTO dto = new PhotoDTO();
        dto.setIdphoto(photo.getIdphoto());
        dto.setLigneId(photo.getLigneCommande() != null ? photo.getLigneCommande().getIdligne() : null);
        dto.setUrl("/api/photos/" + photo.getIdphoto() + "/fichier");
        dto.setTypephoto(photo.getTypephoto());
        dto.setDateTime(photo.getDateTime());
        return dto;
    }

    private TarifResumeDTO toTarifResume(Tarif tarif) {
        if (tarif == null) {
            return null;
        }
        TarifResumeDTO dto = new TarifResumeDTO();
        dto.setIdtarif(tarif.getIdtarif());
        dto.setNom(tarif.getNom());
        dto.setTypevetement(tarif.getTypevetement());
        dto.setPrixunitaire(tarif.getPrixunitaire());
        dto.setPrixauklo(tarif.getPrixauklo());
        dto.setTypeNettoyage(tarif.getTypeNettoyage());
        return dto;
    }

    private PaiementResumeDTO toPaiementDTO(Paiement paiement) {
        PaiementResumeDTO dto = new PaiementResumeDTO();
        dto.setIdpaiement(paiement.getIdpaiement());
        dto.setMontant(paiement.getMontant());
        dto.setMoyenPaiement(paiement.getMoyenPaiement());
        dto.setTypePaiement(paiement.getTypePaiement());
        dto.setReferenceTransaction(paiement.getReferenceTransaction());
        dto.setDatePaiement(paiement.getDatePaiement());
        return dto;
    }
}
