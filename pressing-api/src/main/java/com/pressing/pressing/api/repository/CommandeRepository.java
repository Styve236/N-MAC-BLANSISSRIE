package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.Commande;
import com.pressing.pressing.api.entite.StatutCommande;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;




@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long>, JpaSpecificationExecutor<Commande> {
    List<Commande> findByClientIdclient(Long clientId);

    Page<Commande> findByStatut(StatutCommande statut, Pageable pageable);

    Page<Commande> findByClientIdclient(Long clientId, Pageable pageable);

    Page<Commande> findByClientIdclientAndStatut(Long clientId, StatutCommande statut, Pageable pageable);

    Page<Commande> findByDateCreationBetween(LocalDateTime debut, LocalDateTime fin, Pageable pageable);

    long countByDateCreationBetween(LocalDateTime debut, LocalDateTime fin);

    long countByStatut(StatutCommande statut);

    @Query("SELECT c FROM Commande c WHERE COALESCE(c.montantTotal, 0) - COALESCE(c.remise, 0) > COALESCE(c.montantPaye, 0)")
    List<Commande> findByMontantPayeLessThanMontantTotal();

    // Agregats en base : le rapport ne doit pas charger toute la table pour compter
    @Query("SELECT c.statut, COUNT(c) FROM Commande c WHERE c.statut IS NOT NULL GROUP BY c.statut")
    List<Object[]> countParStatut();

    // Meme predicat que findByMontantPayeLessThanMontantTotal, agrege en SQL
    @Query("SELECT COUNT(c), COALESCE(SUM(COALESCE(c.montantTotal, 0) - COALESCE(c.remise, 0) "
            + "- COALESCE(c.montantPaye, 0)), 0) FROM Commande c "
            + "WHERE COALESCE(c.montantTotal, 0) - COALESCE(c.remise, 0) > COALESCE(c.montantPaye, 0)")
    List<Object[]> countEtSommeImpayees();

    @Query("SELECT c FROM Commande c WHERE c.statut = :statut AND (c.adresseLivraison IS NULL OR c.livreur IS NULL)")
    List<Commande> findPretesSansLivraison(@Param("statut") StatutCommande statut);

    @Query("SELECT c FROM Commande c WHERE c.dateRecuperationPrevue IS NOT NULL "
            + "AND c.dateRecuperationPrevue < :limite "
            + "AND c.statut NOT IN :terminees "
            + "ORDER BY c.dateRecuperationPrevue ASC")
    List<Commande> findEnRetard(@Param("limite") LocalDateTime limite,
            @Param("terminees") List<StatutCommande> terminees);

    Optional<Commande> findByNumeroTicket(String numeroTicket);
}