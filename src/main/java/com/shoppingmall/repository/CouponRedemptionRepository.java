package com.shoppingmall.repository;

import com.shoppingmall.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    @Query("""
            select count(distinct r.checkout.id) from CouponRedemption r
            where r.coupon.id = :couponId and r.customer.id = :customerId
            """)
    long countDistinctCheckouts(@Param("couponId") Long couponId, @Param("customerId") Long customerId);

    @Query("select coalesce(sum(r.discountAmount), 0) from CouponRedemption r where r.coupon.id = :couponId")
    java.math.BigDecimal sumDiscount(@Param("couponId") Long couponId);
}
