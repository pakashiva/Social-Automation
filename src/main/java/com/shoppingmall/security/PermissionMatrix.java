package com.shoppingmall.security;

import java.util.List;

public final class PermissionMatrix {

    private PermissionMatrix() {
    }

    public static List<String> admin() {
        return List.of(
                "Users, shops, products, categories, orders, payments, coupons",
                "Customers, feedback, staff, attendance, salaries",
                "Parking, complaints, reports, notifications, roles, audit logs, settings");
    }

    public static List<String> shopOwner() {
        return List.of(
                "Own shop profile, products, inventory, and shop coupons",
                "Own orders and fulfillment",
                "Own reports and notifications");
    }

    public static List<String> customer() {
        return List.of(
                "Profile, cart, checkout, orders, and simulated payment",
                "Wishlist, invoices, reviews, feedback, complaints",
                "Own notifications");
    }
}
