package com.shoppingmall.dto;


import com.shoppingmall.validation.MobileNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter @Setter
public class ShopProfileForm {
    @Size(max = 2000)
    private String description;
    @NotBlank @MobileNumber
    private String contactPhone;
    @NotNull
    private LocalTime openingTime;
    @NotNull
    private LocalTime closingTime;
    private MultipartFile logo;
}
