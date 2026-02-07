package movieapp.repository;

import movieapp.entity.BlockedKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedKeywordRepository extends JpaRepository<BlockedKeyword, Long>, JpaSpecificationExecutor<BlockedKeyword> {
    Optional<BlockedKeyword> findByKeywordIgnoreCase(String keyword);

    boolean existsByKeywordIgnoreCase(String keyword);

    @Query("SELECT b.keyword FROM BlockedKeyword b WHERE b.isActive = true")
    List<String> findAllActiveKeyword();
}
