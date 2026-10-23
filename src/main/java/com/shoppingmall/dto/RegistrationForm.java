package com.shoppingmall.dto;


import com.shoppingmall.validation.GstNumber;
import com.shoppingmall.validation.MobileNumber;
import com.shoppingmall.validation.PasswordRules;
import com.shoppingmall.validation.ShopOwnerFields;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ShopOwnerFields
public class RegistrationForm {
    @NotBlank
    private String accountType;
    @NotBlank
    @Size(max = 120)
    private String fullName;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    @MobileNumber
    private String mobile;
    @NotBlank
    @PasswordRules
    private String password;
    private String confirmPassword;
    private String businessName;
    @GstNumber
    private String gstNumber;
    private String shopName;
    private String floorLabel;
    private String unitNumber;
    private String shopPhone;
    private LocalTime openingTime;
    private LocalTime closingTime;
}
