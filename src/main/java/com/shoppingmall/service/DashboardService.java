package com.shoppingmall.service;

import com.shoppingmall.dto.DashboardView;

public interface DashboardService {

    DashboardView customer(long userId);

    DashboardView owner(long userId);

    DashboardView admin();
}
