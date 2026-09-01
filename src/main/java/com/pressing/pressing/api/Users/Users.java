package com.pressing.pressing.api.Users;


import jakarta.persistence.*;
import lombok.*;

import com.pressing.pressing.api.Users.Role;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users {
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long idusers;
    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private  String tels;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean actif = true;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

}
