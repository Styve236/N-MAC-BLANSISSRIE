package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.ClotureCaisse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;




public interface ClotureCaisseRepository extends JpaRepository<ClotureCaisse, Long> {
    Optional<ClotureCaisse> findTopByDateOrderByDateClotureDesc(LocalDate date);

    List<ClotureCaisse> findAllByOrderByDateClotureDesc();
}