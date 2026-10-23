package com.shoppingmall.service.impl;

import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.ActivityLog;
import com.shoppingmall.repository.ActivityLogRepository;
import com.shoppingmall.repository.UserRepository;
import com.shoppingmall.service.AuditService;
import com.shoppingmall.util.Pages;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditServiceImpl implements AuditService {

    private final ActivityLogRepository logs;
    private final UserRepository users;
    private final Clock clock;

    public AuditServiceImpl(ActivityLogRepository logs, UserRepository users, Clock clock) {
        this.logs = logs;
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void record(Long actorUserId, String action, String entityType, Long entityId, String summary) {
        ActivityLog row = new ActivityLog();
        if (actorUserId != null) {
            row.setActor(users.getReferenceById(actorUserId));
        }
        row.setAction(action);
        row.setEntityType(entityType);
        row.setEntityId(entityId);
        String text = summary == null ? "" : summary;
        row.setSummary(text.length() > 500 ? text.substring(0, 500) : text);
        row.setIpAddress(ip());
        row.setCreatedAt(LocalDateTime.now(clock));
        logs.save(row);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.ActivityRow> search(String action, Long actorId, int page) {
        Page<ActivityLog> found;
        boolean hasAction = action != null && !action.isBlank();
        if (hasAction && actorId != null) {
            found = logs.findByActionAndActorIdOrderByCreatedAtDesc(action, actorId, Pages.of(page));
        } else if (hasAction) {
            found = logs.findByActionOrderByCreatedAtDesc(action, Pages.of(page));
        } else if (actorId != null) {
            found = logs.findByActorIdOrderByCreatedAtDesc(actorId, Pages.of(page));
        } else {
            found = logs.findAllByOrderByCreatedAtDesc(Pages.of(page));
        }
        return found.map(this::row);
    }

    private Rows.ActivityRow row(ActivityLog log) {
        Rows.ActivityRow row = new Rows.ActivityRow();
        row.setActor(log.getActor() == null ? "" : log.getActor().getFullName());
        row.setAction(log.getAction());
        row.setEntityType(log.getEntityType());
        row.setEntityId(log.getEntityId());
        row.setSummary(log.getSummary());
        row.setIpAddress(log.getIpAddress());
        row.setCreatedAt(log.getCreatedAt());
        return row;
    }

    private String ip() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String raw = request.getHeader("X-Forwarded-For");
        if (raw == null || raw.isBlank()) {
            raw = request.getRemoteAddr();
        } else {
            raw = raw.split(",")[0].trim();
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.length() > 45 ? raw.substring(0, 45) : raw;
    }
}
