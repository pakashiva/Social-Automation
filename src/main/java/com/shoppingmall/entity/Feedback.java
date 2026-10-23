package com.shoppingmall.entity;

import com.shoppingmall.enums.FeedbackSource;
import com.shoppingmall.enums.FeedbackStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "feedback")
public class Feedback extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private FeedbackSource source;

    @Column(name = "guest_name", length = 120)
    private String guestName;

    @Column(name = "guest_email", length = 255)
    private String guestEmail;

    @Column(name = "subject", nullable = false, length = 160)
    private String subject;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "rating")
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeedbackStatus status;
}
