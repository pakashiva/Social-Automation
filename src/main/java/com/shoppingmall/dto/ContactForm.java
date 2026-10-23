package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter @Setter
public class ContactForm {
    @NotBlank @Size(max = 120)
    private String guestName;
    @NotBlank @Email
    private String guestEmail;
    @NotBlank @Size(max = 160)
    private String subject;
    @NotBlank @Size(min = 10, max = 2000, message = "Write at least 10 characters.")
    private String message;
}
