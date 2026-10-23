package com.shoppingmall.repository;

import com.shoppingmall.entity.Role;
import com.shoppingmall.enums.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
