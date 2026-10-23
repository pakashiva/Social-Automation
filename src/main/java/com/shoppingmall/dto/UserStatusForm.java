package com.shoppingmall.dto;


import com.shoppingmall.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserStatusForm {
    @NotNull
    private UserStatus status;
}
