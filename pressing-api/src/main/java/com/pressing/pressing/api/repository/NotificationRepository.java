package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.Notification;
import com.pressing.pressing.api.entite.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRoleCibleOrderByDateCreationDesc(Role roleCible);
}