package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.SmsNotification;
import org.springframework.data.jpa.repository.JpaRepository;



public interface SmsNotificationRepository extends JpaRepository<SmsNotification, Long> {
}