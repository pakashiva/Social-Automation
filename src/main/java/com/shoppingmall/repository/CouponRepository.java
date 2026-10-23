package com.shoppingmall.repository;

import com.shoppingmall.entity.Coupon;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.code = :code")
    Optional<Coupon> findByCode(@Param("code") String code);

    Optional<Coupon> findFirstByCode(String code);

    boolean existsByCode(String code);

    @Query("""
            select c from Coupon c
            where c.active = true and c.startsAt <= :now and c.endsAt >= :now
            order by c.code
            """)
    List<Coupon> findPublic(@Param("now") LocalDateTime now);

    Page<Coupon> findByShopIdIn(List<Long> shopIds, Pageable pageable);
    Page<Coupon> findAllByOrderByCodeAsc(Pageable pageable);
}
