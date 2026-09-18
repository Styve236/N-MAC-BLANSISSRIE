package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.Tarif;
import com.pressing.pressing.api.entite.TypeNettoyage;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;




@Repository
public interface TarifRepository extends JpaRepository<Tarif, Long> {

    List<Tarif> findByActifTrue();

    Page<Tarif> findByTypeNettoyage(TypeNettoyage typeNettoyage, Pageable pageable);
}