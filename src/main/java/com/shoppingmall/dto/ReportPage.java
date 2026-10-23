package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter @Setter
public class ReportPage {
    private List<Map<String, String>> sales = new ArrayList<>();
    private String successTotal = "0.00";
    private String refundedTotal = "0.00";
    private String netTotal = "0.00";
    private List<Map<String, String>> stock = new ArrayList<>();
    private List<Map<String, String>> coupons = new ArrayList<>();
    private List<Map<String, String>> parking = new ArrayList<>();
    private List<Map<String, String>> attendance = new ArrayList<>();
    private List<Map<String, String>> topProducts = new ArrayList<>();
    private boolean empty = true;
}
