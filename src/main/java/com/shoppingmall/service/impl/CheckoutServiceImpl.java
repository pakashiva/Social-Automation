package com.shoppingmall.service.impl;

import com.shoppingmall.dto.CartView;
import com.shoppingmall.dto.OrderView;
import com.shoppingmall.entity.Cart;
import com.shoppingmall.entity.CartItem;
import com.shoppingmall.entity.Checkout;
import com.shoppingmall.entity.Coupon;
import com.shoppingmall.entity.CouponRedemption;
import com.shoppingmall.entity.Customer;
import com.shoppingmall.entity.Inventory;
import com.shoppingmall.entity.MallOrder;
import com.shoppingmall.entity.OrderItem;
import com.shoppingmall.entity.OrderStatusHistory;
import com.shoppingmall.entity.Payment;
import com.shoppingmall.entity.Product;
import com.shoppingmall.enums.CartStatus;
import com.shoppingmall.enums.CouponRejectReason;
import com.shoppingmall.enums.CouponScope;
import com.shoppingmall.enums.DiscountType;
import com.shoppingmall.enums.OrderStatus;
import com.shoppingmall.enums.PaymentMethod;
import com.shoppingmall.enums.PaymentStatus;
import com.shoppingmall.enums.StockMovementType;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.CouponRejectedException;
import com.shoppingmall.exception.ForbiddenException;
import com.shoppingmall.exception.InsufficientStockException;
import com.shoppingmall.repository.CartRepository;
import com.shoppingmall.repository.CheckoutRepository;
import com.shoppingmall.repository.CouponRedemptionRepository;
import com.shoppingmall.repository.CouponRepository;
import com.shoppingmall.repository.InventoryRepository;
import com.shoppingmall.repository.MallOrderRepository;
import com.shoppingmall.repository.OrderStatusHistoryRepository;
import com.shoppingmall.repository.PaymentRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.CheckoutService;
import com.shoppingmall.service.NumberSequenceService;
import com.shoppingmall.util.Money;
import com.shoppingmall.util.MoneySplit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private final OwnershipGuard guard;
    private final CartRepository carts;
    private final CartServiceImpl cartService;
    private final InventoryRepository inventories;
    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final CheckoutRepository checkouts;
    private final MallOrderRepository orders;
    private final PaymentRepository payments;
    private final OrderStatusHistoryRepository history;
    private final NumberSequenceService sequences;
    private final StockLedger ledger;
    private final OrderMapper orderMapper;
    private final EntityManager entityManager;
    private final Clock clock;
    private final TransactionTemplate transactions;

    public CheckoutServiceImpl(
            OwnershipGuard guard,
            CartRepository carts,
            CartServiceImpl cartService,
            InventoryRepository inventories,
            CouponRepository coupons,
            CouponRedemptionRepository redemptions,
            CheckoutRepository checkouts,
            MallOrderRepository orders,
            PaymentRepository payments,
            OrderStatusHistoryRepository history,
            NumberSequenceService sequences,
            StockLedger ledger,
            OrderMapper orderMapper,
            EntityManager entityManager,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.guard = guard;
        this.carts = carts;
        this.cartService = cartService;
        this.inventories = inventories;
        this.coupons = coupons;
        this.redemptions = redemptions;
        this.checkouts = checkouts;
        this.orders = orders;
        this.payments = payments;
        this.history = history;
        this.sequences = sequences;
        this.ledger = ledger;
        this.orderMapper = orderMapper;
        this.entityManager = entityManager;
        this.clock = clock;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public CartView preview(long userId, String couponCode) {
        Customer customer = guard.requireCustomer(userId);
        requireAddress(customer);
        Cart cart = carts.findByCustomerIdAndStatus(customer.getId(), CartStatus.OPEN)
                .orElseThrow(() -> new BusinessException("Your cart is empty.", "/customer/cart"));
        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Your cart is empty.", "/customer/cart");
        }
        List<Draft> drafts = quote(customer, cart.getItems(), couponCode, false);
        CartView view = new CartView();
        view.setAddressComplete(true);
        for (Draft draft : drafts) {
            CartView.Line line = new CartView.Line();
            line.setProductId(draft.product.getId());
            line.setProductName(draft.product.getName());
            line.setShopName(draft.product.getShop().getName());
            line.setQuantity(draft.quantity);
            line.setUnitPrice(draft.unitPrice);
            line.setLineTotal(draft.lineTotal);
            view.getLines().add(line);
            view.setSubtotal(view.getSubtotal().add(draft.subtotal));
            view.setDiscount(view.getDiscount().add(draft.discount));
            view.setTax(view.getTax().add(draft.tax));
        }
        view.setSubtotal(Money.scale(view.getSubtotal()));
        view.setDiscount(Money.scale(view.getDiscount()));
        view.setTax(Money.scale(view.getTax()));
        view.setTotal(view.getSubtotal().subtract(view.getDiscount()).add(view.getTax()));
        view.setCouponCode(couponCode);
        return view;
    }

    @Override
    public List<OrderView> place(long userId, String checkoutToken, String couponCode, String customerNote) {
        if (checkoutToken == null || checkoutToken.length() != 36) {
            throw new BusinessException("Checkout was opened in another tab. Start checkout again.", null, 400);
        }
        try {
            return transactions.execute(status -> write(userId, checkoutToken, couponCode, customerNote));
        } catch (RuntimeException exception) {
            if (duplicateCheckout(exception)) {
                return transactions.execute(status -> loadExisting(userId, checkoutToken));
            }
            throw exception;
        }
    }

    private List<OrderView> write(long userId, String checkoutToken, String couponCode, String customerNote) {
        Customer customer = guard.requireCustomer(userId);
        requireAddress(customer);
        Cart open = carts.findByCustomerIdAndStatus(customer.getId(), CartStatus.OPEN)
                .orElseThrow(() -> new BusinessException("Your cart is empty.", "/customer/cart"));
        if (open.getItems().isEmpty()) {
            throw new BusinessException("Your cart is empty.", "/customer/cart");
        }
        for (CartItem item : open.getItems()) {
            if (!ProductServiceImpl.sellable(item.getProduct())) {
                throw new BusinessException(
                        "Remove " + item.getProduct().getName() + " from the cart before checkout.", "/customer/cart");
            }
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Checkout checkout = new Checkout();
        checkout.setCheckoutNumber(sequences.next("CHECKOUT"));
        checkout.setCustomer(customer);
        checkout.setIdempotencyKey(checkoutToken);
        checkout.setCreatedAt(now);
        checkouts.saveAndFlush(checkout);

        Cart cart = carts.lockById(open.getId()).orElseThrow();
        if (cart.getStatus() != CartStatus.OPEN || cart.getItems().isEmpty()) {
            throw new BusinessException("Your cart is empty.", "/customer/cart");
        }
        List<Long> productIds = cart.getItems().stream().map(item -> item.getProduct().getId()).sorted().toList();
        Map<Long, Inventory> locked = new LinkedHashMap<>();
        for (Long productId : productIds) {
            Object rawId;
            try {
                rawId = entityManager.createNativeQuery("select id from inventory where product_id = ?1 for update")
                        .setParameter(1, productId)
                        .getSingleResult();
            } catch (jakarta.persistence.NoResultException exception) {
                throw new BusinessException("That product is not available.", "/customer/cart");
            }
            long inventoryId = ((Number) rawId).longValue();
            Inventory loaded = entityManager.find(Inventory.class, inventoryId);
            if (loaded != null) {
                entityManager.detach(loaded);
            }
            Inventory inventory = entityManager.find(Inventory.class, inventoryId, LockModeType.PESSIMISTIC_WRITE);
            if (inventory == null) {
                throw new BusinessException("That product is not available.", "/customer/cart");
            }
            if (inventory.getProduct() != null) {
                inventory.getProduct().setInventory(inventory);
            }
            locked.put(productId, inventory);
        }
        List<Draft> drafts = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            entityManager.refresh(product);
            if (!ProductServiceImpl.sellable(product)) {
                throw new BusinessException(
                        "Remove " + product.getName() + " from the cart before checkout.", "/customer/cart");
            }
            Inventory inventory = locked.get(product.getId());
            if (item.getQuantity() > inventory.getQuantityOnHand()) {
                throw new InsufficientStockException(
                        "Only " + inventory.getQuantityOnHand() + " of " + product.getName() + " are in stock.");
            }
            Draft draft = new Draft();
            draft.product = product;
            draft.inventory = inventory;
            draft.quantity = item.getQuantity();
            draft.unitPrice = Money.scale(product.getUnitPrice());
            draft.subtotal = Money.scale(draft.unitPrice.multiply(BigDecimal.valueOf(draft.quantity)));
            drafts.add(draft);
        }
        if (couponCode != null && !couponCode.isBlank()) {
            applyCoupon(customer, drafts, couponCode.trim().toUpperCase(), true);
        } else {
            finishLines(drafts);
        }
        Map<Long, List<Draft>> byShop = new LinkedHashMap<>();
        drafts.stream().sorted(Comparator.comparing(draft -> draft.product.getShop().getId()))
                .forEach(draft -> byShop.computeIfAbsent(draft.product.getShop().getId(), id -> new ArrayList<>()).add(draft));
        List<MallOrder> created = new ArrayList<>();
        Coupon coupon = drafts.get(0).coupon;
        for (List<Draft> shopLines : byShop.values()) {
            created.add(createOrder(checkout, customer, shopLines, checkoutToken, customerNote, now, coupon));
        }
        if (coupon != null) {
            coupon.setUsageCount(coupon.getUsageCount() + 1);
            for (MallOrder order : created) {
                if (order.getDiscountAmount().signum() > 0) {
                    CouponRedemption redemption = new CouponRedemption();
                    redemption.setCoupon(coupon);
                    redemption.setOrder(order);
                    redemption.setCustomer(customer);
                    redemption.setCheckout(checkout);
                    redemption.setDiscountAmount(order.getDiscountAmount());
                    redemption.setRedeemedAt(now);
                    redemptions.save(redemption);
                }
            }
        }
        cart.setStatus(CartStatus.CHECKED_OUT);
        return created.stream().map(orderMapper::toView).toList();
    }

    private MallOrder createOrder(
            Checkout checkout,
            Customer customer,
            List<Draft> lines,
            String token,
            String customerNote,
            LocalDateTime now,
            Coupon coupon) {
        BigDecimal subtotal = Money.ZERO;
        BigDecimal discount = Money.ZERO;
        BigDecimal tax = Money.ZERO;
        BigDecimal total = Money.ZERO;
        for (Draft line : lines) {
            subtotal = subtotal.add(line.subtotal);
            discount = discount.add(line.discount);
            tax = tax.add(line.tax);
            total = total.add(line.lineTotal);
        }
        subtotal = Money.scale(subtotal);
        discount = Money.scale(discount);
        tax = Money.scale(tax);
        total = Money.scale(total);
        BigDecimal expected = subtotal.subtract(discount).add(tax);
        if (expected.compareTo(total) != 0) {
            throw new BusinessException("Order totals do not add up.", "/customer/checkout");
        }
        MallOrder order = new MallOrder();
        order.setOrderNumber(sequences.next("ORDER"));
        order.setCheckout(checkout);
        order.setCustomer(customer);
        order.setShop(lines.get(0).product.getShop());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setIdempotencyKey(token + ":" + order.getShop().getId());
        order.setCurrencyCode("INR");
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setTaxAmount(tax);
        order.setTotalAmount(total);
        if (coupon != null && discount.signum() > 0) {
            order.setCoupon(coupon);
            order.setCouponCodeSnapshot(coupon.getCode());
        }
        if (customerNote != null && !customerNote.isBlank()) {
            order.setCustomerNote(customerNote.trim());
        }
        order.setPlacedAt(now);
        for (Draft line : lines) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(line.product);
            item.setProductNameSnapshot(line.product.getName());
            item.setSkuSnapshot(line.product.getSku());
            item.setQuantity(line.quantity);
            item.setUnitPrice(line.unitPrice);
            item.setTaxRatePercent(line.product.getTaxRatePercent());
            item.setDiscountAmount(line.discount);
            item.setTaxAmount(line.tax);
            item.setLineTotal(line.lineTotal);
            item.setCreatedAt(now);
            order.getItems().add(item);
        }
        orders.saveAndFlush(order);
        OrderStatusHistory row = new OrderStatusHistory();
        row.setOrder(order);
        row.setFromStatus(null);
        row.setToStatus(OrderStatus.PENDING_PAYMENT);
        row.setChangedBy(customer.getUser());
        row.setCreatedAt(now);
        history.save(row);
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentNumber(sequences.next("PAYMENT"));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMethod(PaymentMethod.SIMULATED_CARD);
        payment.setAmount(total);
        payment.setCurrencyCode("INR");
        payments.save(payment);
        for (Draft line : lines) {
            ledger.apply(line.inventory, -line.quantity, line.quantity, StockMovementType.RESERVE, "ORDER", order.getId(),
                    "Reserved for " + order.getOrderNumber(), customer.getUser());
        }
        return order;
    }

    private List<Draft> quote(Customer customer, List<CartItem> items, String couponCode, boolean locking) {
        List<Draft> drafts = new ArrayList<>();
        for (CartItem item : items) {
            Product product = item.getProduct();
            if (!ProductServiceImpl.sellable(product)) {
                throw new BusinessException(
                        "Remove " + product.getName() + " from the cart before checkout.", "/customer/cart");
            }
            Draft draft = new Draft();
            draft.product = product;
            draft.quantity = item.getQuantity();
            draft.unitPrice = Money.scale(product.getUnitPrice());
            draft.subtotal = Money.scale(draft.unitPrice.multiply(BigDecimal.valueOf(draft.quantity)));
            drafts.add(draft);
        }
        if (couponCode != null && !couponCode.isBlank()) {
            applyCoupon(customer, drafts, couponCode.trim().toUpperCase(), locking);
        } else {
            finishLines(drafts);
        }
        return drafts;
    }

    private void applyCoupon(Customer customer, List<Draft> drafts, String code, boolean locking) {
        Coupon coupon = (locking ? coupons.findByCode(code) : coupons.findFirstByCode(code))
                .orElseThrow(() -> reject(CouponRejectReason.NOT_FOUND, "That coupon code was not found."));
        if (!coupon.isActive()) {
            throw reject(CouponRejectReason.INACTIVE, "That coupon is not active.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(coupon.getStartsAt())) {
            throw reject(CouponRejectReason.NOT_STARTED, "That coupon has not started.");
        }
        if (now.isAfter(coupon.getEndsAt())) {
            throw reject(CouponRejectReason.EXPIRED, "That coupon has expired.");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            throw reject(CouponRejectReason.USAGE_LIMIT, "That coupon has reached its usage limit.");
        }
        long used = redemptions.countDistinctCheckouts(coupon.getId(), customer.getId());
        if (used >= coupon.getPerCustomerLimit()) {
            throw reject(CouponRejectReason.CUSTOMER_LIMIT, "You have already used that coupon.");
        }
        List<Draft> eligible = drafts.stream().filter(draft -> matches(coupon, draft)).toList();
        if (eligible.isEmpty()) {
            throw reject(CouponRejectReason.NOT_ELIGIBLE, "That coupon does not apply to this cart.");
        }
        BigDecimal eligibleSubtotal = Money.scale(eligible.stream().map(draft -> draft.subtotal)
                .reduce(Money.ZERO, BigDecimal::add));
        if (eligibleSubtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw reject(CouponRejectReason.MINIMUM,
                    "The minimum order for that coupon is ₹" + coupon.getMinOrderAmount().setScale(2) + ".");
        }
        BigDecimal discount = coupon.getDiscountType() == DiscountType.PERCENT
                ? MoneySplit.percentDiscount(eligibleSubtotal, coupon.getDiscountValue(), coupon.getMaxDiscountAmount())
                : MoneySplit.fixedDiscount(eligibleSubtotal, coupon.getDiscountValue());
        Map<Long, BigDecimal> shares = MoneySplit.split(discount, eligible.stream()
                .map(draft -> new MoneySplit.Line(draft.product.getId(), draft.subtotal)).toList());
        for (Draft draft : drafts) {
            draft.discount = shares.getOrDefault(draft.product.getId(), Money.ZERO);
            draft.coupon = coupon;
        }
        finishLines(drafts);
    }

    private static void finishLines(List<Draft> drafts) {
        for (Draft draft : drafts) {
            if (draft.discount == null) {
                draft.discount = Money.ZERO;
            }
            BigDecimal net = draft.subtotal.subtract(draft.discount);
            draft.tax = MoneySplit.tax(net, draft.product.getTaxRatePercent());
            draft.lineTotal = Money.scale(net.add(draft.tax));
        }
    }

    private static boolean matches(Coupon coupon, Draft draft) {
        if (coupon.getScope() == CouponScope.MALL) {
            return true;
        }
        if (coupon.getScope() == CouponScope.SHOP) {
            return coupon.getShop() != null && coupon.getShop().getId().equals(draft.product.getShop().getId());
        }
        return coupon.getCategory() != null && coupon.getCategory().getId().equals(draft.product.getCategory().getId());
    }

    private List<OrderView> loadExisting(long userId, String token) {
        Checkout checkout = checkouts.findByIdempotencyKey(token)
                .orElseThrow(() -> new BusinessException("Checkout was opened in another tab. Start checkout again.", null, 400));
        if (!checkout.getCustomer().getUser().getId().equals(userId)) {
            throw new ForbiddenException();
        }
        return orders.findByCheckoutIdOrderByIdAsc(checkout.getId()).stream().map(orderMapper::toView).toList();
    }

    private static void requireAddress(Customer customer) {
        if (blank(customer.getAddressLine1()) || blank(customer.getCity())
                || blank(customer.getState()) || blank(customer.getPostalCode())) {
            throw new BusinessException("Add an address to your profile before checkout.", "/customer/profile");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static CouponRejectedException reject(CouponRejectReason reason, String message) {
        return new CouponRejectedException(reason, message);
    }

    private static boolean duplicateCheckout(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null && message.contains("uk_checkouts_key")) {
                return true;
            }
        }
        return false;
    }

    private static final class Draft {
        private Product product;
        private Inventory inventory;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private BigDecimal discount = Money.ZERO;
        private BigDecimal tax = Money.ZERO;
        private BigDecimal lineTotal = Money.ZERO;
        private Coupon coupon;
    }
}
