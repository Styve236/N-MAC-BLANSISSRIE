package com.pressing.pressing.api.inventaire;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventaireRepository extends JpaRepository<Inventaire, Long> {
    List<Inventaire> findAllByOrderByDateInventaireDesc();
}