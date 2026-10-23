package com.shoppingmall.dto;


import com.shoppingmall.enums.ComplaintCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ComplaintForm {
    @NotNull
    private ComplaintCategory category;
    @NotBlank @Size(max = 160)
    private String subject;
    @NotBlank @Size(min = 10, max = 2000)
    private String description;
    private String orderNumber;
    private Long shopId;
}
