package com.pressing.pressing.api.dto.response;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlerteCommandeRetardDTO {
    private Long commandeId;
    private String numeroTicket;
    private String clientNom;
    private String clientTelephone;
    private LocalDate dateRecuperationPrevue;
    private long joursRetard;
}