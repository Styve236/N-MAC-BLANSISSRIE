package com.pressing.pressing.api.dto.response;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;




@Data
public class ClotureDTO {
    private Long idcloture;
    private LocalDate date;
    private LocalDateTime dateCloture;
    private BigDecimal totalLogiciel;
    private BigDecimal totalCompte;
    private BigDecimal ecart;
    private Integer nombrePaiements;
    private List<MontantParMoyenDTO> parMoyen;
    private String observations;
    private boolean cloturee;
}