package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.HistoriquePoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;




public interface HistoriquePointRepository extends JpaRepository<HistoriquePoint, Long> {
    List<HistoriquePoint> findByClientIdOrderByDateDesc(Long clientId);
}