package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class ShopCard {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String floorLabel;
    private String unitNumber;
    private String phone;
    private String email;
    private String openingTime;
    private String closingTime;
    private String hours;
    private String status;
    private String logoUrl;
    private String ownerName;
    private String businessName;
    private String gstNumber;
    private String approvalStatus;
}
