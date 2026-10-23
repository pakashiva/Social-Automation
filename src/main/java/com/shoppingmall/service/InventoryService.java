package com.shoppingmall.service;

import com.shoppingmall.dto.Rows;
import com.shoppingmall.dto.StockAdjustForm;
import org.springframework.data.domain.Page;

public interface InventoryService {

    Page<Rows.InventoryRow> listForOwner(long userId, int page);

    void adjust(long userId, long productId, StockAdjustForm form);
}
