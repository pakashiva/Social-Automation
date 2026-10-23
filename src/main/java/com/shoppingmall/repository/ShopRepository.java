package com.shoppingmall.repository;

import com.shoppingmall.entity.Shop;
import com.shoppingmall.enums.ShopStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    Optional<Shop> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByFloorLabelAndUnitNumber(String floorLabel, String unitNumber);
    List<Shop> findByOwnerId(Long ownerId);
    Page<Shop> findByStatus(ShopStatus status, Pageable pageable);
    List<Shop> findByStatusOrderByNameAsc(ShopStatus status);
    Page<Shop> findAllByOrderByNameAsc(Pageable pageable);

    @Query("select count(s) from Shop s where s.owner.user.id = :userId")
    long countByOwnerUserId(@Param("userId") Long userId);
}
