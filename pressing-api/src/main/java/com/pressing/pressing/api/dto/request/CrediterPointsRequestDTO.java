package com.pressing.pressing.api.dto.request;
import lombok.Data;



@Data
public class CrediterPointsRequestDTO {
    private Long clientId;
    private Integer nbPoints;
    private String description;
}