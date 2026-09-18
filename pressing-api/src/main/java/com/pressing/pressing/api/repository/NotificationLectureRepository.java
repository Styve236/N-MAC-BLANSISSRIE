package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.NotificationLecture;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLectureRepository extends JpaRepository<NotificationLecture, Long> {
    Optional<NotificationLecture> findByNotificationIdAndUtilisateurId(Long notificationId, Long utilisateurId);

    @Modifying
    @Query("UPDATE NotificationLecture l SET l.lue = true WHERE l.utilisateurId = :utilisateurId AND l.lue = false")
    int marquerToutesLues(@Param("utilisateurId") Long utilisateurId);
}