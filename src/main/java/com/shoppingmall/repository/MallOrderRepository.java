package com.shoppingmall.repository;

import com.shoppingmall.entity.MallOrder;
import com.shoppingmall.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MallOrderRepository extends JpaRepository<MallOrder, Long> {
    Optional<MallOrder> findByOrderNumber(String orderNumber);
    List<MallOrder> findByCheckoutIdOrderByIdAsc(Long checkoutId);
    Page<MallOrder> findByCustomerIdOrderByPlacedAtDesc(Long customerId, Pageable pageable);
    Page<MallOrder> findByShopIdInOrderByPlacedAtDesc(Collection<Long> shopIds, Pageable pageable);
    Page<MallOrder> findAllByOrderByPlacedAtDesc(Pageable pageable);
    List<MallOrder> findTop5ByCustomerIdOrderByPlacedAtDesc(Long customerId);

    @Query("select o.status, count(o) from MallOrder o where o.shop.id in :shopIds group by o.status")
    List<Object[]> countByStatusForShops(@Param("shopIds") Collection<Long> shopIds);

    long countByPlacedAtGreaterThanEqualAndPlacedAtLessThan(LocalDateTime start, LocalDateTime end);

    @Query("select o.status, count(o) from MallOrder o group by o.status")
    List<Object[]> countByStatus();

    @Query("""
            select o from MallOrder o
            join fetch o.shop
            join fetch o.customer c
            join fetch c.user
            where o.placedAt >= :start and o.placedAt < :end
            order by o.placedAt asc, o.id asc
            """)
    List<MallOrder> findSales(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select o from MallOrder o
            join fetch o.shop
            join fetch o.customer c
            join fetch c.user
            where o.placedAt >= :start and o.placedAt < :end
              and o.status = :status
            order by o.placedAt asc, o.id asc
            """)
    List<MallOrder> findSalesByStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") OrderStatus status);

    @Query("""
            select o from MallOrder o
            join fetch o.shop
            join fetch o.customer c
            join fetch c.user
            where o.placedAt >= :start and o.placedAt < :end
              and o.shop.id in :shopIds
            order by o.placedAt asc, o.id asc
            """)
    List<MallOrder> findSalesForShops(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("shopIds") Collection<Long> shopIds);

    @Query("""
            select o from MallOrder o
            join fetch o.shop
            join fetch o.customer c
            join fetch c.user
            where o.placedAt >= :start and o.placedAt < :end
              and o.shop.id in :shopIds
              and o.status = :status
            order by o.placedAt asc, o.id asc
            """)
    List<MallOrder> findSalesForShopsAndStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("shopIds") Collection<Long> shopIds,
            @Param("status") OrderStatus status);
}
