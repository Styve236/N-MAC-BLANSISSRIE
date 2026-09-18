package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.LigneCommande;
import org.springframework.data.jpa.repository.JpaRepository;



public interface LigneCommandeRepository extends JpaRepository<LigneCommande, Long> {
}