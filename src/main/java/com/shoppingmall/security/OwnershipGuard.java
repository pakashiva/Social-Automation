package com.shoppingmall.security;

import com.shoppingmall.entity.Coupon;
import com.shoppingmall.entity.Customer;
import com.shoppingmall.entity.MallOrder;
import com.shoppingmall.entity.Product;
import com.shoppingmall.entity.Shop;
import com.shoppingmall.entity.ShopOwner;
import com.shoppingmall.entity.User;
import com.shoppingmall.enums.ApprovalStatus;
import com.shoppingmall.enums.RoleName;
import com.shoppingmall.enums.UserStatus;
import com.shoppingmall.exception.ForbiddenException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.CouponRepository;
import com.shoppingmall.repository.CustomerRepository;
import com.shoppingmall.repository.MallOrderRepository;
import com.shoppingmall.repository.ProductRepository;
import com.shoppingmall.repository.ShopOwnerRepository;
import com.shoppingmall.repository.ShopRepository;
import com.shoppingmall.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class OwnershipGuard {

    private final UserRepository users;
    private final CustomerRepository customers;
    private final ShopOwnerRepository owners;
    private final ShopRepository shops;
    private final ProductRepository products;
    private final MallOrderRepository orders;
    private final CouponRepository coupons;

    public OwnershipGuard(
            UserRepository users,
            CustomerRepository customers,
            ShopOwnerRepository owners,
            ShopRepository shops,
            ProductRepository products,
            MallOrderRepository orders,
            CouponRepository coupons) {
        this.users = users;
        this.customers = customers;
        this.owners = owners;
        this.shops = shops;
        this.products = products;
        this.orders = orders;
        this.coupons = coupons;
    }

    public User requireUser(long userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User was not found."));
    }

    public Customer requireCustomer(long userId) {
        User user = requireUser(userId);
        if (user.getUserRole() == null || user.getUserRole().getRole().getName() != RoleName.CUSTOMER) {
            throw new ForbiddenException();
        }
        return customers.findByUserId(userId).orElseThrow(ForbiddenException::new);
    }

    public ShopOwner requireOwner(long userId) {
        User user = requireUser(userId);
        if (user.getStatus() != UserStatus.ACTIVE
                || user.getUserRole() == null
                || user.getUserRole().getRole().getName() != RoleName.SHOP_OWNER) {
            throw new ForbiddenException();
        }
        ShopOwner owner = owners.findByUserId(userId).orElseThrow(ForbiddenException::new);
        if (owner.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ForbiddenException();
        }
        return owner;
    }

    public boolean isAdmin(long userId) {
        User user = requireUser(userId);
        return user.getUserRole() != null && user.getUserRole().getRole().getName() == RoleName.ADMIN
                && user.getStatus() == UserStatus.ACTIVE;
    }

    public Shop requireOwnShop(long userId, long shopId) {
        ShopOwner owner = requireOwner(userId);
        Shop shop = shops.findById(shopId).orElseThrow(() -> new NotFoundException("Shop was not found."));
        if (!shop.getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenException();
        }
        return shop;
    }

    public Product requireOwnProduct(long userId, long productId) {
        ShopOwner owner = requireOwner(userId);
        Product product = products.findById(productId).orElseThrow(() -> new NotFoundException("Product was not found."));
        if (!product.getShop().getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenException();
        }
        return product;
    }

    public MallOrder requireOwnOrder(long userId, String orderNumber) {
        ShopOwner owner = requireOwner(userId);
        MallOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order was not found."));
        if (!order.getShop().getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenException();
        }
        return order;
    }

    public Coupon requireOwnCoupon(long userId, long couponId) {
        ShopOwner owner = requireOwner(userId);
        Coupon coupon = coupons.findById(couponId).orElseThrow(() -> new NotFoundException("Coupon was not found."));
        if (coupon.getShop() == null || !coupon.getShop().getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenException();
        }
        return coupon;
    }

    public MallOrder requireOwnCustomerOrder(long userId, String orderNumber) {
        Customer customer = requireCustomer(userId);
        MallOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order was not found."));
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException();
        }
        return order;
    }
}
