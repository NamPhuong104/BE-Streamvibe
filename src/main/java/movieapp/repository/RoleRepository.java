package movieapp.repository;

import movieapp.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {
    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    Set<Role> findByNameIn(Set<String> name);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.id = :roleId")
    long countUsersByRoleId(Long roleId);

    List<Role> findAllByOrderByPriorityAsc();

    List<Role> findByPriorityBetween(Integer minPriority, Integer maxPriority);
}
