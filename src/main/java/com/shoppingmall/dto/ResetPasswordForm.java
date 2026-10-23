package com.shoppingmall.dto;


import com.shoppingmall.validation.PasswordRules;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResetPasswordForm {
    @NotBlank
    private String token;
    @NotBlank
    @PasswordRules
    private String password;
    private String confirmPassword;
}
