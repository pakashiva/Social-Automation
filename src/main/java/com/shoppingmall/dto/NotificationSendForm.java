package com.shoppingmall.dto;


import com.shoppingmall.enums.NotificationAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class NotificationSendForm {
    @NotNull
    private NotificationAudience audience;
    private Long userId;
    @NotBlank @Size(max = 160)
    private String title;
    @NotBlank @Size(max = 1000)
    private String body;
}
