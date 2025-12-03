package movieapp.repository;

import movieapp.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    boolean existsByEmail(String email);

    Optional<User> findByRefreshTokenAndEmail(String refreshToken, String email);

    Optional<User> findByResetPasswordToken(String token);

    Optional<User> findByVerifyEmailToken(String token);

    Optional<User> findByChangeEmailToken(String token);
}
