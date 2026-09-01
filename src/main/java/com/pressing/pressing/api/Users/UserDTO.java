package com.pressing.pressing.api.Users;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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