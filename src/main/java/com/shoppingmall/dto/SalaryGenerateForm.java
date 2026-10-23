package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Getter @Setter
public class SalaryGenerateForm {
    @NotBlank @Pattern(regexp = "^[0-9]{4}-[0-9]{2}$")
    private String yearMonth;
}
