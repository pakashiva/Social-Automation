package com.shoppingmall.service;

import com.shoppingmall.dto.OrderView;
import com.shoppingmall.enums.OrderStatus;
import org.springframework.data.domain.Page;

public interface OrderService {

    Page<OrderView> listForCustomer(long userId, int page);

    OrderView getForCustomer(long userId, String orderNumber);

    Page<OrderView> listForOwner(long userId, int page);

    OrderView getForOwner(long userId, String orderNumber);

    Page<OrderView> listAll(int page);

    OrderView get(String orderNumber);

    void changeStatus(long userId, String orderNumber, OrderStatus status);

    void cancelByCustomer(long userId, String orderNumber, String reason);

    void cancelByOwner(long userId, String orderNumber, String reason);

    void cancelByAdmin(long adminId, String orderNumber, String reason);
}
