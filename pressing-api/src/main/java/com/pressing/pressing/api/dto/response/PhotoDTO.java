package com.pressing.pressing.api.dto.response;
import java.time.LocalDateTime;
import lombok.Data;




@Data
public class PhotoDTO {
    private Long idphoto;
    private Long ligneId;
    private String url;
    private String typephoto;
    private LocalDateTime dateTime;
}