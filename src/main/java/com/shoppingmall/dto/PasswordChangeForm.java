package com.shoppingmall.dto;


import com.shoppingmall.validation.PasswordRules;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PasswordChangeForm {
    @NotBlank
    private String currentPassword;
    @NotBlank
    @PasswordRules
    private String newPassword;
    private String confirmPassword;
}
