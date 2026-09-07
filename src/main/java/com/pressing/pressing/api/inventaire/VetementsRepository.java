package com.pressing.pressing.api.inventaire;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VetementsRepository extends JpaRepository<Vetements, Long> {
    List<Vetements> findAllByOrderByLibelleAsc();
}