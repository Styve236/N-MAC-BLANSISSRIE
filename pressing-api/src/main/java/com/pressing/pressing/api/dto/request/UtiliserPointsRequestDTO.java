package com.pressing.pressing.api.dto.request;
import lombok.Data;



@Data
public class UtiliserPointsRequestDTO {
    private Long clientId;
    private Long commandeId;
    private Integer nbPoints;
}