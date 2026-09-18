package com.pressing.pressing.api.dto.response;
import com.pressing.pressing.api.entite.TypeNotificationInterne;
import java.time.LocalDateTime;
import lombok.Data;


@Data
public class NotificationDTO {
    private Long id;
    private String message;
    private Long commandeId;
    private String numeroTicket;
    private TypeNotificationInterne type;
    private LocalDateTime dateCreation;
    private boolean lue;
}