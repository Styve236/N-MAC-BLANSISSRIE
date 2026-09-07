package com.pressing.pressing.api.fidelite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistoriquePointRepository extends JpaRepository<HistoriquePoint, Long> {
    List<HistoriquePoint> findByClientIdOrderByDateDesc(Long clientId);

    @Query("SELECT COALESCE(SUM(h.points), 0) FROM HistoriquePoint h WHERE h.clientId = :clientId")
    Integer sommePoints(@Param("clientId") Long clientId);
}