package com.pressing.pressing.api.entite;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pressing.pressing.api.entite.Role;
import jakarta.persistence.*;
import lombok.*;
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

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean actif = true;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

}
