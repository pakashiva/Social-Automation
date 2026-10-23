package com.shoppingmall.service.impl;

import com.shoppingmall.dto.ChartSeries;
import com.shoppingmall.dto.ReportPage;
import com.shoppingmall.entity.Attendance;
import com.shoppingmall.entity.Coupon;
import com.shoppingmall.entity.Employee;
import com.shoppingmall.entity.Inventory;
import com.shoppingmall.entity.MallOrder;
import com.shoppingmall.entity.ParkingSession;
import com.shoppingmall.entity.Payment;
import com.shoppingmall.entity.Shop;
import com.shoppingmall.enums.OrderStatus;
import com.shoppingmall.enums.ParkingSessionStatus;
import com.shoppingmall.enums.PaymentStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.repository.AttendanceRepository;
import com.shoppingmall.repository.CouponRedemptionRepository;
import com.shoppingmall.repository.CouponRepository;
import com.shoppingmall.repository.EmployeeRepository;
import com.shoppingmall.repository.InventoryRepository;
import com.shoppingmall.repository.MallOrderRepository;
import com.shoppingmall.repository.OrderItemRepository;
import com.shoppingmall.repository.ParkingSessionRepository;
import com.shoppingmall.repository.PaymentRepository;
import com.shoppingmall.repository.ShopRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.ReportService;
import com.shoppingmall.util.Money;
import com.shoppingmall.util.TimeDisplay;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    private final OwnershipGuard guard;
    private final ShopRepository shops;
    private final MallOrderRepository orders;
    private final OrderItemRepository items;
    private final PaymentRepository payments;
    private final InventoryRepository inventories;
    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final ParkingSessionRepository sessions;
    private final EmployeeRepository employees;
    private final AttendanceRepository attendance;
    private final java.time.Clock clock;

    public ReportServiceImpl(
            OwnershipGuard guard,
            ShopRepository shops,
            MallOrderRepository orders,
            OrderItemRepository items,
            PaymentRepository payments,
            InventoryRepository inventories,
            CouponRepository coupons,
            CouponRedemptionRepository redemptions,
            ParkingSessionRepository sessions,
            EmployeeRepository employees,
            AttendanceRepository attendance,
            java.time.Clock clock) {
        this.guard = guard;
        this.shops = shops;
        this.orders = orders;
        this.items = items;
        this.payments = payments;
        this.inventories = inventories;
        this.coupons = coupons;
        this.redemptions = redemptions;
        this.sessions = sessions;
        this.employees = employees;
        this.attendance = attendance;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportPage adminReport(LocalDate from, LocalDate to, Long shopId, String orderStatus) {
        Range range = range(from, to, "/admin/reports");
        List<Long> shopIds = shopId == null ? null : List.of(shopId);
        ReportPage page = sales(shopIds, range, orderStatus, true);
        page.setStock(stock(null));
        page.setCoupons(couponUsage(null));
        page.setParking(closedParking(range));
        page.setAttendance(attendance(range.from()));
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public ChartSeries adminChart() {
        return chart(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportPage ownerReport(long userId, LocalDate from, LocalDate to, String orderStatus) {
        List<Long> shopIds = ownerShops(userId);
        Range range = range(from, to, "/owner/reports");
        ReportPage page = sales(shopIds, range, orderStatus, false);
        page.setTopProducts(topProducts(shopIds, range));
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public ChartSeries ownerChart(long userId) {
        return chart(ownerShops(userId));
    }

    private List<Long> ownerShops(long userId) {
        return shops.findByOwnerId(guard.requireOwner(userId).getId()).stream().map(Shop::getId).toList();
    }

    private ReportPage sales(List<Long> shopIds, Range range, String orderStatus, boolean admin) {
        ReportPage page = new ReportPage();
        List<MallOrder> found = matchingSales(shopIds, range, parseStatus(orderStatus));
        Map<Long, PaymentStatus> paymentStatus = paymentStatusByOrder(found);
        List<Map<String, String>> rows = new ArrayList<>();
        for (MallOrder order : found) {
            PaymentStatus status = paymentStatus.get(order.getId());
            Map<String, String> row = new LinkedHashMap<>();
            row.put("orderNumber", order.getOrderNumber());
            row.put("shop", order.getShop().getName());
            row.put("customer", order.getCustomer().getUser().getFullName());
            row.put("orderStatus", order.getStatus().name());
            row.put("paymentStatus", status == null ? "" : status.name());
            row.put("subtotal", plain(order.getSubtotal()));
            row.put("discount", plain(order.getDiscountAmount()));
            row.put("tax", plain(order.getTaxAmount()));
            row.put("total", plain(order.getTotalAmount()));
            rows.add(row);
        }
        page.setSales(rows);
        page.setEmpty(rows.isEmpty());
        BigDecimal success = money(shopIds, range, true);
        BigDecimal refunded = money(shopIds, range, false);
        page.setSuccessTotal(plain(success));
        page.setRefundedTotal(plain(refunded));
        page.setNetTotal(plain(success.subtract(refunded)));
        if (admin) {
            return page;
        }
        return page;
    }

    private List<MallOrder> matchingSales(List<Long> shopIds, Range range, OrderStatus status) {
        if (shopIds != null && shopIds.isEmpty()) {
            return List.of();
        }
        if (shopIds == null) {
            return status == null
                    ? orders.findSales(range.start(), range.end())
                    : orders.findSalesByStatus(range.start(), range.end(), status);
        }
        return status == null
                ? orders.findSalesForShops(range.start(), range.end(), shopIds)
                : orders.findSalesForShopsAndStatus(range.start(), range.end(), shopIds, status);
    }

    private Map<Long, PaymentStatus> paymentStatusByOrder(List<MallOrder> found) {
        if (found.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = found.stream().map(MallOrder::getId).toList();
        Map<Long, PaymentStatus> byOrder = new LinkedHashMap<>();
        for (Object[] row : payments.findStatusByOrderIdIn(ids)) {
            byOrder.put((Long) row[0], (PaymentStatus) row[1]);
        }
        return byOrder;
    }

    private BigDecimal money(List<Long> shopIds, Range range, boolean success) {
        if (shopIds != null && shopIds.isEmpty()) {
            return Money.ZERO;
        }
        if (success) {
            if (shopIds == null) {
                return Money.scale(payments.sumPaid(PaymentStatus.SUCCESS, range.start(), range.end()));
            }
            return Money.scale(payments.sumPaidForShops(PaymentStatus.SUCCESS, range.start(), range.end(), shopIds));
        }
        if (shopIds == null) {
            return Money.scale(payments.sumRefunded(range.start(), range.end()));
        }
        return Money.scale(payments.sumRefundedForShops(range.start(), range.end(), shopIds));
    }

    private List<Map<String, String>> stock(Collection<Long> shopIds) {
        List<Inventory> found = shopIds == null ? inventories.findAll() : inventories.findForShops(shopIds);
        List<Map<String, String>> rows = new ArrayList<>();
        for (Inventory inventory : found) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("sku", inventory.getProduct().getSku());
            row.put("name", inventory.getProduct().getName());
            row.put("shop", inventory.getProduct().getShop().getName());
            row.put("onHand", Integer.toString(inventory.getQuantityOnHand()));
            row.put("reserved", Integer.toString(inventory.getQuantityReserved()));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, String>> couponUsage(Collection<Long> shopIds) {
        List<Map<String, String>> rows = new ArrayList<>();
        for (Coupon coupon : coupons.findAll()) {
            if (shopIds != null && (coupon.getShop() == null || !shopIds.contains(coupon.getShop().getId()))) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            row.put("code", coupon.getCode());
            row.put("usageCount", Integer.toString(coupon.getUsageCount()));
            row.put("discountSum", plain(redemptions.sumDiscount(coupon.getId())));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, String>> closedParking(Range range) {
        List<Map<String, String>> rows = new ArrayList<>();
        BigDecimal sum = Money.ZERO;
        for (ParkingSession session : sessions.findAll()) {
            if (session.getStatus() != ParkingSessionStatus.CLOSED || session.getExitAt() == null) {
                continue;
            }
            if (!inRange(session.getExitAt(), range)) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            row.put("ticket", session.getTicketNumber());
            row.put("vehicle", session.getVehicleNumber());
            row.put("charged", plain(session.getChargedAmount()));
            rows.add(row);
            if (session.getChargedAmount() != null) {
                sum = sum.add(session.getChargedAmount());
            }
        }
        if (!rows.isEmpty()) {
            Map<String, String> total = new LinkedHashMap<>();
            total.put("ticket", "Total");
            total.put("vehicle", "");
            total.put("charged", plain(sum));
            rows.add(total);
        }
        return rows;
    }

    private List<Map<String, String>> attendance(LocalDate from) {
        YearMonth month = YearMonth.from(from);
        List<Map<String, String>> rows = new ArrayList<>();
        for (Employee employee : employees.findAll()) {
            List<Attendance> found = attendance.findByEmployeeIdAndWorkDateBetween(
                    employee.getId(), month.atDay(1), month.atEndOfMonth());
            Map<String, String> row = new LinkedHashMap<>();
            row.put("employee", employee.getFullName());
            row.put("count", Integer.toString(found.size()));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, String>> topProducts(List<Long> shopIds, Range range) {
        if (shopIds.isEmpty()) {
            return List.of();
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (Object[] row : items.topByQuantity(shopIds, range.start(), range.end(), PageRequest.of(0, 5))) {
            Map<String, String> mapped = new LinkedHashMap<>();
            mapped.put("name", String.valueOf(row[1]));
            mapped.put("quantity", Long.toString(((Number) row[2]).longValue()));
            rows.add(mapped);
        }
        return rows;
    }

    private ChartSeries chart(List<Long> shopIds) {
        LocalDate today = LocalDate.now(clock.withZone(TimeDisplay.ZONE));
        LocalDate startDay = today.minusDays(13);
        Range range = new Range(startDay, today, utcStart(startDay), utcStart(today.plusDays(1)));
        Map<LocalDate, BigDecimal> totals = new LinkedHashMap<>();
        for (int i = 0; i < 14; i++) {
            totals.put(startDay.plusDays(i), Money.ZERO);
        }
        if (shopIds == null || !shopIds.isEmpty()) {
            List<Payment> found = shopIds == null
                    ? payments.successBetween(range.start(), range.end())
                    : payments.successBetweenForShops(range.start(), range.end(), shopIds);
            for (Payment payment : found) {
                if (payment.getPaidAt() == null) {
                    continue;
                }
                LocalDate day = payment.getPaidAt().atZone(ZoneOffset.UTC).withZoneSameInstant(TimeDisplay.ZONE).toLocalDate();
                if (totals.containsKey(day)) {
                    totals.put(day, totals.get(day).add(payment.getAmount()));
                }
            }
        }
        List<String> labels = new ArrayList<>();
        List<String> amounts = new ArrayList<>();
        totals.forEach((day, amount) -> {
            labels.add(day.format(DAY));
            amounts.add(plain(amount));
        });
        return new ChartSeries(labels, amounts);
    }

    private Range range(LocalDate from, LocalDate to, String redirect) {
        LocalDate today = LocalDate.now(clock.withZone(TimeDisplay.ZONE));
        LocalDate end = to == null ? (from == null ? today : from) : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) {
            throw new BusinessException("The start date must be on or before the end date.", redirect);
        }
        if (ChronoUnit.DAYS.between(start, end) > 366) {
            throw new BusinessException("Choose a range of 366 days or fewer.", redirect);
        }
        return new Range(start, end, utcStart(start), utcStart(end.plusDays(1)));
    }

    private static LocalDateTime utcStart(LocalDate date) {
        return date.atStartOfDay(TimeDisplay.ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private static boolean inRange(LocalDateTime value, Range range) {
        return value != null && !value.isBefore(range.start()) && value.isBefore(range.end());
    }

    private static OrderStatus parseStatus(String orderStatus) {
        if (orderStatus == null || orderStatus.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(orderStatus);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String plain(BigDecimal amount) {
        return Money.scale(amount).toPlainString();
    }

    private record Range(LocalDate from, LocalDate to, LocalDateTime start, LocalDateTime end) {
    }
}
