package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter @Setter
public class ReviewForm {
    @NotNull
    private Long orderItemId;
    @NotNull @Min(1) @Max(5)
    private Integer rating;
    @NotBlank @Size(min = 3, max = 120)
    private String title;
    @NotBlank @Size(min = 10, max = 2000)
    private String body;
}
