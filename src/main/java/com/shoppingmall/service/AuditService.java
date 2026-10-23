package com.shoppingmall.service;

import com.shoppingmall.dto.Rows;
import org.springframework.data.domain.Page;

public interface AuditService {

    void record(Long actorUserId, String action, String entityType, Long entityId, String summary);

    Page<Rows.ActivityRow> search(String action, Long actorId, int page);
}
