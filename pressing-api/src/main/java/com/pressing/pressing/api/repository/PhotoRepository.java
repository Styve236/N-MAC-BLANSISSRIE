package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.Photo;
import org.springframework.data.jpa.repository.JpaRepository;



public interface PhotoRepository extends JpaRepository<Photo, Long> {
}