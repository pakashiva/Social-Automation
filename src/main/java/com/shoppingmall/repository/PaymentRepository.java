package com.shoppingmall.repository;

import com.shoppingmall.entity.Payment;
import com.shoppingmall.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.order.id = :orderId")
    Optional<Payment> findByOrderId(@Param("orderId") Long orderId);

    Optional<Payment> findFirstByOrderId(Long orderId);

    @Query("select p.order.id, p.status from Payment p where p.order.id in :orderIds")
    List<Object[]> findStatusByOrderIdIn(@Param("orderIds") Collection<Long> orderIds);

    Page<Payment> findAllByOrderByIdDesc(Pageable pageable);

    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.status = :status and p.paidAt >= :start and p.paidAt < :end
            """)
    java.math.BigDecimal sumPaid(
            @Param("status") PaymentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.status = :status and p.paidAt >= :start and p.paidAt < :end
              and p.order.shop.id in :shopIds
            """)
    java.math.BigDecimal sumPaidForShops(
            @Param("status") PaymentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("shopIds") Collection<Long> shopIds);

    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.status = com.shoppingmall.enums.PaymentStatus.REFUNDED
              and p.refundedAt >= :start and p.refundedAt < :end
            """)
    java.math.BigDecimal sumRefunded(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.status = com.shoppingmall.enums.PaymentStatus.REFUNDED
              and p.refundedAt >= :start and p.refundedAt < :end
              and p.order.shop.id in :shopIds
            """)
    java.math.BigDecimal sumRefundedForShops(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("shopIds") Collection<Long> shopIds);

    @Query("""
            select p from Payment p
            where p.status = com.shoppingmall.enums.PaymentStatus.SUCCESS
              and p.paidAt >= :start and p.paidAt < :end
            """)
    List<Payment> successBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select p from Payment p
            where p.status = com.shoppingmall.enums.PaymentStatus.SUCCESS
              and p.paidAt >= :start and p.paidAt < :end
              and p.order.shop.id in :shopIds
            """)
    List<Payment> successBetweenForShops(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("shopIds") Collection<Long> shopIds);
}
