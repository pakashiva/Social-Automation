package com.shoppingmall.service;

import com.shoppingmall.dto.ChartSeries;
import com.shoppingmall.dto.ReportPage;
import java.time.LocalDate;

public interface ReportService {

    ReportPage adminReport(LocalDate from, LocalDate to, Long shopId, String orderStatus);

    ChartSeries adminChart();

    ReportPage ownerReport(long userId, LocalDate from, LocalDate to, String orderStatus);

    ChartSeries ownerChart(long userId);
}
