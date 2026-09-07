package com.pressing.pressing.api.caisse;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClotureCaisseRepository extends JpaRepository<ClotureCaisse, Long> {
    Optional<ClotureCaisse> findTopByDateOrderByDateClotureDesc(LocalDate date);

    List<ClotureCaisse> findAllByOrderByDateClotureDesc();
}