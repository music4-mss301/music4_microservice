package vn.edu.fpt.mss.userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.mss.userservice.entity.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUserNameIgnoreCase(String userName);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUserNameIgnoreCase(String userName);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    List<User> findByIdIn(Collection<Integer> ids);

    long countByRoleAndIsDeletedFalse(String role);

    long countByRoleAndEnabledTrueAndIsDeletedFalse(String role);

    long countByIsDeletedFalse();

    @Query("""
            SELECT u FROM User u
            WHERE u.isDeleted = false
              AND (:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:role IS NULL OR u.role = :role)
            """)
    Page<User> search(@Param("keyword") String keyword, @Param("role") String role, Pageable pageable);
}
