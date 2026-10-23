package com.shoppingmall.repository;

import com.shoppingmall.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ActivityLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
    Page<ActivityLog> findByActorIdOrderByCreatedAtDesc(Long actorId, Pageable pageable);
    Page<ActivityLog> findByActionAndActorIdOrderByCreatedAtDesc(String action, Long actorId, Pageable pageable);
    java.util.List<ActivityLog> findTop10ByOrderByCreatedAtDesc();
}
