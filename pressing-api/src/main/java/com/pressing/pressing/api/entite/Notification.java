package com.pressing.pressing.api.entite;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "notification_interne")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String message;

    private Long commandeId;
    private String numeroTicket;

    @Enumerated(EnumType.STRING)
    private TypeNotificationInterne type;

    @Enumerated(EnumType.STRING)
    private Role roleCible;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationLecture> lectures = new ArrayList<>();
}