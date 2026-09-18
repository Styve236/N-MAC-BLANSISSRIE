package com.pressing.pressing.api.dto.response;
import com.pressing.pressing.api.entite.Role;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;




@Getter
@Setter
public class UserDTO {
    private Long idusers;
    private String nom;
    private String tels;
    private String email;
    private Role role;
    private boolean actif;
    private LocalDateTime dateCreation;
}