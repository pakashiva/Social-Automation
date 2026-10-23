package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter @Setter
public class CategoryForm {
    private Long id;
    @NotBlank @Size(max = 80)
    private String name;
    @Size(max = 500)
    private String description;
    private Long parentId;
    private boolean active = true;
    @NotNull @Min(0)
    private Integer displayOrder;
}
