package com.shoppingmall.support;

import com.shoppingmall.entity.Cart;
import com.shoppingmall.entity.CartItem;
import com.shoppingmall.entity.Category;
import com.shoppingmall.entity.Coupon;
import com.shoppingmall.entity.Customer;
import com.shoppingmall.entity.Inventory;
import com.shoppingmall.entity.Product;
import com.shoppingmall.entity.Role;
import com.shoppingmall.entity.Shop;
import com.shoppingmall.entity.ShopOwner;
import com.shoppingmall.entity.User;
import com.shoppingmall.entity.UserRole;
import com.shoppingmall.enums.ApprovalStatus;
import com.shoppingmall.enums.CartStatus;
import com.shoppingmall.enums.CouponScope;
import com.shoppingmall.enums.DiscountType;
import com.shoppingmall.enums.ProductStatus;
import com.shoppingmall.enums.RoleName;
import com.shoppingmall.enums.ShopStatus;
import com.shoppingmall.enums.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import com.shoppingmall.repository.CartItemRepository;
import com.shoppingmall.repository.CartRepository;
import com.shoppingmall.repository.CategoryRepository;
import com.shoppingmall.repository.CouponRepository;
import com.shoppingmall.repository.CustomerRepository;
import com.shoppingmall.repository.InventoryRepository;
import com.shoppingmall.repository.ProductRepository;
import com.shoppingmall.repository.RoleRepository;
import com.shoppingmall.repository.ShopOwnerRepository;
import com.shoppingmall.repository.ShopRepository;
import com.shoppingmall.repository.UserRepository;
import com.shoppingmall.repository.UserRoleRepository;

@Component
public class TestData {

    public static final String PASSWORD = "Testpass1";
    private static final String HASH = new BCryptPasswordEncoder(10).encode(PASSWORD);
    private static final AtomicInteger SEQUENCE = new AtomicInteger(1000);

    private final TransactionTemplate tx;
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final CustomerRepository customers;
    private final ShopOwnerRepository owners;
    private final ShopRepository shops;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final InventoryRepository inventories;
    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final CouponRepository coupons;

    public TestData(
            TransactionTemplate tx,
            JdbcTemplate jdbc,
            UserRepository users,
            RoleRepository roles,
            UserRoleRepository userRoles,
            CustomerRepository customers,
            ShopOwnerRepository owners,
            ShopRepository shops,
            CategoryRepository categories,
            ProductRepository products,
            InventoryRepository inventories,
            CartRepository carts,
            CartItemRepository cartItems,
            CouponRepository coupons) {
        this.tx = tx;
        this.jdbc = jdbc;
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
        this.customers = customers;
        this.owners = owners;
        this.shops = shops;
        this.categories = categories;
        this.products = products;
        this.inventories = inventories;
        this.carts = carts;
        this.cartItems = cartItems;
        this.coupons = coupons;
    }

    public record Person(long userId, String email) {
    }

    public record Store(long userId, long shopId, long categoryId) {
    }

    public void ensureRoles() {
        tx.executeWithoutResult(status -> {
            saveRole(RoleName.ADMIN, "Mall administrator");
            saveRole(RoleName.SHOP_OWNER, "Approved shop owner");
            saveRole(RoleName.CUSTOMER, "Shopper");
        });
    }

    public Person person(RoleName role, UserStatus userStatus) {
        int n = SEQUENCE.incrementAndGet();
        return tx.execute(status -> {
            User user = user(n, userStatus);
            assign(user, role);
            return new Person(user.getId(), user.getEmail());
        });
    }

    public Person customer() {
        int n = SEQUENCE.incrementAndGet();
        return tx.execute(status -> {
            User user = user(n, UserStatus.ACTIVE);
            assign(user, RoleName.CUSTOMER);
            Customer customer = new Customer();
            customer.setUser(user);
            customer.setCountry("India");
            customer.setAddressLine1(n + " Residency Road");
            customer.setCity("Bengaluru");
            customer.setState("Karnataka");
            customer.setPostalCode("560001");
            customers.save(customer);
            Cart cart = new Cart();
            cart.setCustomer(customer);
            cart.setStatus(CartStatus.OPEN);
            carts.save(cart);
            return new Person(user.getId(), user.getEmail());
        });
    }

    public Store store() {
        int n = SEQUENCE.incrementAndGet();
        return tx.execute(status -> {
            User user = user(n, UserStatus.ACTIVE);
            assign(user, RoleName.SHOP_OWNER);
            ShopOwner owner = new ShopOwner();
            owner.setUser(user);
            owner.setBusinessName("Business " + n);
            owner.setApprovalStatus(ApprovalStatus.APPROVED);
            owners.save(owner);
            Shop shop = new Shop();
            shop.setOwner(owner);
            shop.setName("Shop " + n);
            shop.setSlug("shop-" + n);
            shop.setDescription("Test shop");
            shop.setFloorLabel("T" + n);
            shop.setUnitNumber(String.valueOf(n));
            shop.setContactPhone("9000000099");
            shop.setContactEmail(user.getEmail());
            shop.setOpeningTime(LocalTime.of(10, 0));
            shop.setClosingTime(LocalTime.of(21, 0));
            shop.setStatus(ShopStatus.ACTIVE);
            shops.save(shop);
            Category category = new Category();
            category.setName("Category " + n);
            category.setSlug("category-" + n);
            category.setActive(true);
            category.setDisplayOrder(n);
            categories.save(category);
            return new Store(user.getId(), shop.getId(), category.getId());
        });
    }

    public long product(Store store, BigDecimal price, BigDecimal tax, int onHand) {
        int n = SEQUENCE.incrementAndGet();
        return tx.execute(status -> {
            Product product = new Product();
            product.setShop(shops.findById(store.shopId()).orElseThrow());
            product.setCategory(categories.findById(store.categoryId()).orElseThrow());
            product.setName("Product " + n);
            product.setSlug("product-" + n);
            product.setSku("SKU-" + n);
            product.setUnitPrice(price);
            product.setTaxRatePercent(tax);
            product.setStatus(ProductStatus.ACTIVE);
            products.save(product);
            Inventory inventory = new Inventory();
            inventory.setProduct(product);
            inventory.setQuantityOnHand(onHand);
            inventory.setQuantityReserved(0);
            inventories.save(inventory);
            return product.getId();
        });
    }

    public void addToCart(long userId, long productId, int quantity) {
        tx.executeWithoutResult(status -> {
            Customer customer = customers.findByUserId(userId).orElseThrow();
            Cart cart = carts.findByCustomerIdAndStatus(customer.getId(), CartStatus.OPEN).orElseThrow();
            Product product = products.findById(productId).orElseThrow();
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPriceSnapshot(product.getUnitPrice());
            cartItems.save(item);
        });
    }

    public String coupon(
            long createdBy,
            CouponScope scope,
            DiscountType type,
            BigDecimal value,
            BigDecimal minimum,
            Integer usageLimit,
            int usageCount,
            Long shopId,
            Long categoryId,
            LocalDateTime starts,
            LocalDateTime ends) {
        int n = SEQUENCE.incrementAndGet();
        String code = "CODE" + n;
        tx.executeWithoutResult(status -> {
            Coupon coupon = new Coupon();
            coupon.setCode(code);
            coupon.setDescription("Test coupon " + n);
            coupon.setDiscountType(type);
            coupon.setDiscountValue(value);
            coupon.setMinOrderAmount(minimum);
            coupon.setUsageLimit(usageLimit);
            coupon.setUsageCount(usageCount);
            coupon.setPerCustomerLimit(1);
            coupon.setStartsAt(starts);
            coupon.setEndsAt(ends);
            coupon.setActive(true);
            coupon.setScope(scope);
            if (shopId != null) {
                coupon.setShop(shops.findById(shopId).orElseThrow());
            }
            if (categoryId != null) {
                coupon.setCategory(categories.findById(categoryId).orElseThrow());
            }
            coupon.setCreatedBy(users.findById(createdBy).orElseThrow());
            coupons.save(coupon);
        });
        return code;
    }

    public String userStatus(long userId) {
        return tx.execute(status -> users.findById(userId).orElseThrow().getStatus().name());
    }

    public String approvalStatus(long userId) {
        return tx.execute(status -> owners.findByUserId(userId).orElseThrow().getApprovalStatus().name());
    }

    public long shopIdForUser(long userId) {
        return tx.execute(status -> {
            ShopOwner owner = owners.findByUserId(userId).orElseThrow();
            return shops.findByOwnerId(owner.getId()).get(0).getId();
        });
    }

    public String shopStatus(long shopId) {
        return tx.execute(status -> shops.findById(shopId).orElseThrow().getStatus().name());
    }

    public int[] stock(long productId) {
        return jdbc.queryForObject(
                "select quantity_on_hand, quantity_reserved from inventory where product_id = ?",
                (rs, row) -> new int[] {rs.getInt(1), rs.getInt(2)},
                productId);
    }

    public long orderId(String orderNumber) {
        return jdbc.queryForObject("select id from orders where order_number = ?", Long.class, orderNumber);
    }

    public String paymentStatus(String orderNumber) {
        return jdbc.queryForObject(
                "select p.status from payments p join orders o on o.id = p.order_id where o.order_number = ?",
                String.class,
                orderNumber);
    }

    public String failureReason(String orderNumber) {
        return jdbc.queryForObject(
                "select p.failure_reason from payments p join orders o on o.id = p.order_id where o.order_number = ?",
                String.class,
                orderNumber);
    }

    public void isolateAdmin(long adminId, Runnable action) {
        List<Long> others = tx.execute(status -> users.findByStatusAndRole(UserStatus.ACTIVE, RoleName.ADMIN).stream()
                .map(User::getId)
                .filter(id -> id != adminId)
                .toList());
        try {
            setStatus(others, UserStatus.INACTIVE);
            action.run();
        } finally {
            setStatus(others, UserStatus.ACTIVE);
        }
    }

    private void setStatus(List<Long> ids, UserStatus userStatus) {
        tx.executeWithoutResult(status -> {
            for (Long id : ids) {
                User user = users.findById(id).orElseThrow();
                user.setStatus(userStatus);
            }
        });
    }

    private void saveRole(RoleName name, String description) {
        if (roles.findByName(name).isPresent()) {
            return;
        }
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        roles.save(role);
    }

    private User user(int n, UserStatus userStatus) {
        User user = new User();
        user.setEmail("user" + n + "@mall.test");
        user.setMobile(String.format("9%09d", n));
        user.setPasswordHash(HASH);
        user.setFullName("Test User " + n);
        user.setStatus(userStatus);
        user.setFailedLoginCount(0);
        return users.save(user);
    }

    private void assign(User user, RoleName name) {
        UserRole link = new UserRole();
        link.setUser(user);
        link.setRole(roles.findByName(name).orElseThrow());
        link.setAssignedAt(LocalDateTime.now());
        userRoles.save(link);
    }
}
