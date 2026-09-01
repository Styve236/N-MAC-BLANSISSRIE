package com.pressing.pressing.api.photo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhotoDTO {
    private Long idphoto;
    private Long ligneId;
    private String url;
    private String typephoto;
    private LocalDateTime dateTime;
}