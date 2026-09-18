package com.pressing.pressing.api.repository;
import com.pressing.pressing.api.entite.Role;
import com.pressing.pressing.api.entite.Users;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;




public interface UserRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Users> findByRoleAndActifTrue(Role role);
}
