package com.shoppingmall.repository;

import com.shoppingmall.entity.OrderItem;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    @Query("""
            select i.product.id, max(i.productNameSnapshot), sum(i.quantity)
            from OrderItem i
            where i.order.shop.id in :shopIds
              and i.order.placedAt >= :start and i.order.placedAt < :end
              and i.order.status <> com.shoppingmall.enums.OrderStatus.CANCELLED
              and i.order.status <> com.shoppingmall.enums.OrderStatus.PAYMENT_FAILED
            group by i.product.id
            order by sum(i.quantity) desc, i.product.id asc
            """)
    List<Object[]> topByQuantity(
            @Param("shopIds") Collection<Long> shopIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}
