package com.shoppingmall.entity;

import com.shoppingmall.enums.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role extends AuditedEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, length = 32)
    private RoleName name;

    @Column(name = "description", nullable = false, length = 255)
    private String description;
}
