package com.shoppingmall.repository;

import com.shoppingmall.entity.Inventory;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.product.id = :productId")
    Optional<Inventory> findByProductId(@Param("productId") Long productId);

    @Query("select i from Inventory i where i.product.shop.id in :shopIds order by i.product.sku")
    java.util.List<Inventory> findForShops(@Param("shopIds") java.util.Collection<Long> shopIds);
}
