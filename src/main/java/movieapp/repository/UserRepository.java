package movieapp.repository;

import movieapp.dto.Dashboard.DailyCountProjection;
import movieapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByEmailOrUsername(String email, String username);

    Optional<User> findByRefreshTokenAndEmail(String refreshToken, String email);

    Optional<User> findByResetPasswordToken(String token);

    Optional<User> findByVerifyEmailToken(String token);

    Optional<User> findByChangeEmailToken(String token);

    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY u.email ASC
            """)
    Page<User> searchByEmailOrUsername(@Param("query") String query, Pageable pageable);

    @Modifying
    @Transactional
    @Query(
            nativeQuery = true,
            value = "UPDATE users SET refresh_token = :refreshToken WHERE email = :email"
    )
    void updateRefreshTokenByEmail(String refreshToken, String email);

    long countByIsActiveTrue();

    long countByIsEmailVerifiedTrue();

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.role.id = :newRoleId WHERE u.role.id = :oldRoleId")
    void updateAllUsersRoleByOldRoleId(@Param("oldRoleId") Long oldRoleId, @Param("newRoleId") Long newRoleId);

    // ==================== DASHBOARD STATISTICS ====================
    long countByIsActiveFalse();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countUserCreatedSince(@Param("since") LocalDateTime since);

    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.createdAt >= :startDate And u.createdAt < :endDate
            """)
    long countUsersCreatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = """
            SELECT DATE(created_at) as date, COUNT(*) as count
            FROM users
            WHERE created_at >= :since
            GROUP BY DATE(created_at)
            ORDER BY date ASC
            """, nativeQuery = true)
    List<DailyCountProjection> countDailyRegistrations(@Param("since") LocalDateTime since);

}
