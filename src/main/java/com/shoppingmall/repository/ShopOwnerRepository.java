package com.shoppingmall.repository;

import com.shoppingmall.entity.ShopOwner;
import com.shoppingmall.enums.ApprovalStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopOwnerRepository extends JpaRepository<ShopOwner, Long> {
    Optional<ShopOwner> findByUserId(Long userId);
    boolean existsByGstNumber(String gstNumber);
    long countByApprovalStatus(ApprovalStatus status);
}
