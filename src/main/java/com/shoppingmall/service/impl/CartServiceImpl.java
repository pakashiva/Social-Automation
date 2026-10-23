package com.shoppingmall.service.impl;

import com.shoppingmall.dto.CartView;
import com.shoppingmall.entity.Cart;
import com.shoppingmall.entity.CartItem;
import com.shoppingmall.entity.Customer;
import com.shoppingmall.entity.Product;
import com.shoppingmall.enums.CartStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.ForbiddenException;
import com.shoppingmall.repository.CartItemRepository;
import com.shoppingmall.repository.CartRepository;
import com.shoppingmall.repository.ProductRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.CartService;
import com.shoppingmall.util.Money;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartServiceImpl implements CartService {

    private final OwnershipGuard guard;
    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductRepository products;

    public CartServiceImpl(
            OwnershipGuard guard,
            CartRepository carts,
            CartItemRepository items,
            ProductRepository products) {
        this.guard = guard;
        this.carts = carts;
        this.items = items;
        this.products = products;
    }

    @Override
    @Transactional
    public CartView view(long userId) {
        Cart cart = openCart(guard.requireCustomer(userId));
        for (CartItem item : cart.getItems()) {
            item.setUnitPriceSnapshot(Money.scale(item.getProduct().getUnitPrice()));
        }
        return toView(cart);
    }

    @Override
    @Transactional
    public void add(long userId, long productId, int quantity) {
        if (quantity < 1 || quantity > 20) {
            throw new BusinessException("Quantity must be from 1 to 20.", "/customer/cart");
        }
        Customer customer = guard.requireCustomer(userId);
        Product product = products.findById(productId)
                .orElseThrow(() -> new BusinessException("That product is not available.", "/customer/cart"));
        if (!ProductServiceImpl.sellable(product)) {
            throw new BusinessException("That product is not available.", "/customer/cart");
        }
        Cart cart = openCart(customer);
        CartItem existing = items.findByCartIdAndProductId(cart.getId(), productId).orElse(null);
        if (existing == null && items.countByCartId(cart.getId()) >= 30) {
            throw new BusinessException("The cart can hold 30 products.", "/customer/cart");
        }
        int next = (existing == null ? 0 : existing.getQuantity()) + quantity;
        if (next > 20) {
            throw new BusinessException("You can keep at most 20 of one product in the cart.", "/customer/cart");
        }
        if (existing == null) {
            existing = new CartItem();
            existing.setCart(cart);
            existing.setProduct(product);
            cart.getItems().add(existing);
        }
        existing.setQuantity(next);
        existing.setUnitPriceSnapshot(Money.scale(product.getUnitPrice()));
        items.save(existing);
    }

    @Override
    @Transactional
    public void updateQuantity(long userId, long itemId, int quantity) {
        if (quantity < 1 || quantity > 20) {
            throw new BusinessException("Quantity must be from 1 to 20.", "/customer/cart");
        }
        CartItem item = items.findByIdAndCartCustomerUserId(itemId, userId).orElseThrow(ForbiddenException::new);
        item.setQuantity(quantity);
        item.setUnitPriceSnapshot(Money.scale(item.getProduct().getUnitPrice()));
    }

    @Override
    @Transactional
    public void remove(long userId, long itemId) {
        CartItem item = items.findByIdAndCartCustomerUserId(itemId, userId).orElseThrow(ForbiddenException::new);
        item.getCart().getItems().remove(item);
        items.delete(item);
    }

    Cart openCart(Customer customer) {
        return carts.findByCustomerIdAndStatus(customer.getId(), CartStatus.OPEN).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setCustomer(customer);
            cart.setStatus(CartStatus.OPEN);
            return carts.save(cart);
        });
    }

    private CartView toView(Cart cart) {
        CartView view = new CartView();
        cart.getItems().stream().sorted(Comparator.comparing(item -> item.getProduct().getName()))
                .forEach(item -> {
                    CartView.Line line = new CartView.Line();
                    line.setId(item.getId());
                    line.setProductId(item.getProduct().getId());
                    line.setProductName(item.getProduct().getName());
                    line.setShopName(item.getProduct().getShop().getName());
                    line.setQuantity(item.getQuantity());
                    line.setUnitPrice(item.getUnitPriceSnapshot());
                    line.setLineTotal(Money.scale(item.getUnitPriceSnapshot().multiply(java.math.BigDecimal.valueOf(item.getQuantity()))));
                    view.getLines().add(line);
                    view.setSubtotal(view.getSubtotal().add(line.getLineTotal()));
                });
        view.setSubtotal(Money.scale(view.getSubtotal()));
        view.setTax(Money.ZERO);
        for (CartItem item : cart.getItems()) {
            var net = Money.scale(item.getUnitPriceSnapshot().multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
            view.setTax(view.getTax().add(com.shoppingmall.util.MoneySplit.tax(net, item.getProduct().getTaxRatePercent())));
        }
        view.setTax(Money.scale(view.getTax()));
        view.setTotal(view.getSubtotal().add(view.getTax()));
        Customer customer = cart.getCustomer();
        view.setAddressComplete(present(customer.getAddressLine1()) && present(customer.getCity())
                && present(customer.getState()) && present(customer.getPostalCode()));
        return view;
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
