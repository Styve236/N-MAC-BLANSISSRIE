package com.pressing.pressing.api.sms;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TypeNotificationSms type;

    private String telephone;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    private StatutEnvoiSms statut;

    @Builder.Default
    private LocalDateTime dateEnvoi = LocalDateTime.now();
}