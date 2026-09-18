package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.Inventaire;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;




public interface InventaireRepository extends JpaRepository<Inventaire, Long> {
    List<Inventaire> findAllByOrderByDateInventaireDesc();
}