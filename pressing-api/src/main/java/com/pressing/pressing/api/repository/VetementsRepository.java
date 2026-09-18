package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.Vetements;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;




public interface VetementsRepository extends JpaRepository<Vetements, Long> {
    List<Vetements> findAllByOrderByLibelleAsc();

    List<Vetements> findByQuantiteStockLessThanEqual(Integer seuil);
}