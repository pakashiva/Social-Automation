package com.shoppingmall.repository;

import com.shoppingmall.entity.User;
import com.shoppingmall.enums.RoleName;
import com.shoppingmall.enums.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);

    @Query("""
            select u from User u
            where lower(u.fullName) like :q or lower(u.email) like :q or u.mobile like :q
            """)
    Page<User> search(@Param("q") String q, Pageable pageable);

    @Query("""
            select count(u) from User u
            where u.status = com.shoppingmall.enums.UserStatus.ACTIVE
              and u.userRole.role.name = com.shoppingmall.enums.RoleName.ADMIN
            """)
    long countActiveAdmins();

    @Query("""
            select u from User u
            where u.status = :status and u.userRole.role.name = :role
            """)
    List<User> findByStatusAndRole(@Param("status") UserStatus status, @Param("role") RoleName role);

    @Query("select count(u) from User u where u.userRole.role.name = :role")
    long countByRole(@Param("role") RoleName role);
}
