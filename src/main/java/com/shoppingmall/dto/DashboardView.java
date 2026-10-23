package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter @Setter
public class DashboardView {
    private Map<String, String> counts = new LinkedHashMap<>();
    private List<OrderView> recentOrders = new ArrayList<>();
    private List<String> activity = new ArrayList<>();
}
