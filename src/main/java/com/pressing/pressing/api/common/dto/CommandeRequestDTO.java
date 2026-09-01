package com.pressing.pressing.api.common.dto;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommandeRequestDTO {
    private Long clientId;
    private List<LigneCommandeDTO> lignes;
    private LocalDateTime dateRetraitPrevue;
}
