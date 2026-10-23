package com.shoppingmall.dto;


import com.shoppingmall.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProfileForm {
    @NotBlank @Size(max = 120)
    private String fullName;
    private Gender gender;
    private LocalDate dateOfBirth;
    @Size(max = 200)
    private String addressLine1;
    @Size(max = 200)
    private String addressLine2;
    @Size(max = 80)
    private String city;
    @Size(max = 80)
    private String state;
    @Size(max = 20)
    private String postalCode;
    @Size(max = 80)
    private String country;
    private String email;
    private String mobile;
}
