package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter @Setter
public class FeedbackForm {
    @NotBlank @Size(max = 160)
    private String subject;
    @NotBlank @Size(min = 10, max = 2000, message = "Write at least 10 characters.")
    private String message;
    @NotNull @Min(1) @Max(5)
    private Integer rating;
}
