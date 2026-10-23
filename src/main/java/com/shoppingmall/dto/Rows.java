package com.shoppingmall.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

public final class Rows {

    private Rows() {
    }

    @Getter
    @Setter
    public static class UserRow {
        private Long id;
        private String email;
        private String mobile;
        private String fullName;
        private String status;
        private String role;
        private String approvalStatus;
        private String businessName;
        private String rejectionReason;
        private String address;
    }

    @Getter
    @Setter
    public static class InventoryRow {
        private Long productId;
        private String sku;
        private String name;
        private String shopName;
        private int onHand;
        private int reserved;
        private boolean lowStock;
    }

    @Getter
    @Setter
    public static class CouponRow {
        private Long id;
        private String code;
        private String description;
        private String discountLabel;
        private String minimum;
        private String endsAt;
        private String scopeLabel;
        private boolean active;
        private String shopName;
    }

    @Getter
    @Setter
    public static class ComplaintRow {
        private String complaintNumber;
        private String category;
        private String subject;
        private String description;
        private String status;
        private String customerName;
        private String resolutionNote;
        private String shopName;
        private String orderNumber;
    }

    @Getter
    @Setter
    public static class EmployeeRow {
        private Long id;
        private String code;
        private String fullName;
        private String email;
        private String mobile;
        private String jobTitle;
        private String department;
        private LocalDate hiredOn;
        private String status;
        private BigDecimal monthlySalary;
    }

    @Getter
    @Setter
    public static class AttendanceRow {
        private Long employeeId;
        private String employeeName;
        private String status;
        private LocalTime checkIn;
        private LocalTime checkOut;
        private String note;
    }

    @Getter
    @Setter
    public static class SalaryRow {
        private Long id;
        private String employeeName;
        private String yearMonth;
        private BigDecimal baseAmount;
        private BigDecimal deductionAmount;
        private BigDecimal netAmount;
        private int presentDays;
        private int absentDays;
        private int halfDays;
        private int leaveDays;
        private String status;
    }

    @Getter
    @Setter
    public static class ActivityRow {
        private String actor;
        private String action;
        private String entityType;
        private Long entityId;
        private String summary;
        private String ipAddress;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    public static class NotificationRow {
        private Long id;
        private String type;
        private String title;
        private String body;
        private String linkPath;
        private boolean unread;
        private LocalDateTime createdAt;
        private String userEmail;
    }

    @Getter
    @Setter
    public static class FeedbackRow {
        private Long id;
        private String source;
        private String subject;
        private String message;
        private Integer rating;
        private String status;
        private String fromName;
    }

    @Getter
    @Setter
    public static class CategoryRow {
        private Long id;
        private String name;
        private String slug;
        private String description;
        private Long parentId;
        private String parentName;
        private boolean active;
        private int displayOrder;
    }

    @Getter
    @Setter
    public static class PaymentRow {
        private String paymentNumber;
        private String orderNumber;
        private String status;
        private String method;
        private BigDecimal amount;
        private String outcome;
        private LocalDateTime paidAt;
    }

    @Getter
    @Setter
    public static class ParkingSlotRow {
        private Long id;
        private String slotCode;
        private String zone;
        private String vehicleType;
        private String status;
    }

    @Getter
    @Setter
    public static class ParkingSessionRow {
        private String ticketNumber;
        private String slotCode;
        private String vehicleNumber;
        private String vehicleType;
        private LocalDateTime entryAt;
        private LocalDateTime exitAt;
        private BigDecimal chargedAmount;
        private String status;
    }

    @Getter
    @Setter
    public static class RoleCount {
        private String name;
        private String description;
        private long users;
        private java.util.List<String> permissions;
    }

    @Getter
    @Setter
    public static class WishlistRow {
        private Long id;
        private Long productId;
        private String productName;
        private String priceLabel;
        private String imageUrl;
    }

    @Getter
    @Setter
    public static class NamedOption {
        private Long id;
        private String name;
    }

    public record CouponChoices(java.util.List<NamedOption> shops, java.util.List<NamedOption> categories) {
    }

    @Getter
    @Setter
    public static class ParkingRateRow {
        private String vehicleType;
        private int graceMinutes;
        private BigDecimal hourlyRate;
        private BigDecimal dailyCap;
    }

    @Getter
    @Setter
    public static class ParkingBoard {
        private java.util.List<ParkingSlotRow> slots = new java.util.ArrayList<>();
        private java.util.List<ParkingSessionRow> openSessions = new java.util.ArrayList<>();
        private java.util.List<ParkingRateRow> rates = new java.util.ArrayList<>();
    }
}
