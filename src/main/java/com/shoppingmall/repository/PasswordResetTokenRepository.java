package com.shoppingmall.repository;

import com.shoppingmall.entity.PasswordResetToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Query("select t from PasswordResetToken t where t.user.id = :userId and t.usedAt is null")
    List<PasswordResetToken> findUnused(@Param("userId") Long userId);
}
