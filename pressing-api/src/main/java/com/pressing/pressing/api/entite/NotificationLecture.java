package com.pressing.pressing.api.entite;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "notification_lecture")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id")
    private Notification notification;

    @Column(nullable = false)
    private Long utilisateurId;

    @Builder.Default
    private boolean lue = false;
}