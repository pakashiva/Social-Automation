package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter @Setter
public class ForgotPasswordForm {
    @NotBlank @Email
    private String email;
}
