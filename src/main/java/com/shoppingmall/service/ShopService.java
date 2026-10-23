package com.shoppingmall.service;

import com.shoppingmall.dto.ShopCard;
import com.shoppingmall.dto.ShopProfileForm;
import com.shoppingmall.enums.ShopStatus;
import java.util.List;
import org.springframework.data.domain.Page;

public interface ShopService {

    List<ShopCard> featured();

    Page<ShopCard> listPublic(int page);

    ShopCard getPublicBySlug(String slug);

    Page<ShopCard> listAll(int page, ShopStatus status);

    ShopCard get(long id);

    void approve(long adminId, long shopId);

    void reject(long adminId, long shopId, String rejectionReason);

    void changeStatus(long adminId, long shopId, ShopStatus status);

    List<ShopCard> listForOwner(long userId);

    void updateOwn(long userId, long shopId, ShopProfileForm form);

    String logoPath(long shopId);
}
