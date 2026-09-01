package com.pressing.pressing.api.sms;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsNotificationRepository extends JpaRepository<SmsNotification, Long> {
}