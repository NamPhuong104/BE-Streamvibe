package movieapp.repository;

import movieapp.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {
    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    List<Role> findAllByOrderByPriorityAsc();

    List<Role> findByPriorityBetween(Integer minPriority, Integer maxPriority);

    // ⭐ SỬA: Đơn giản hóa query cho single role
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.id = :roleId")
    Long countUsersByRoleId(@Param("roleId") Long roleId);

    // ⭐ THÊM: Lấy roles mà user có thể assign (priority cao hơn)
    @Query("SELECT r FROM Role r WHERE r.priority > :userPriority ORDER BY r.priority ASC")
    List<Role> findAssignableRoles(@Param("userPriority") Integer userPriority);

    // ⭐ THÊM: Lấy admin roles
    @Query("SELECT r FROM Role r WHERE r.priority <= 10 ORDER BY r.priority ASC")
    List<Role> findAdminRoles();

    // ⭐ THÊM: Lấy moderator roles trở lên
    @Query("SELECT r FROM Role r WHERE r.priority <= 50 ORDER BY r.priority ASC")
    List<Role> findModeratorAndAboveRoles();
}
