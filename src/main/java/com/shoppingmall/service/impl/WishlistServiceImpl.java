package com.shoppingmall.service.impl;

import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.Customer;
import com.shoppingmall.entity.Product;
import com.shoppingmall.entity.Wishlist;
import com.shoppingmall.entity.WishlistItem;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.ProductRepository;
import com.shoppingmall.repository.WishlistItemRepository;
import com.shoppingmall.repository.WishlistRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.CartService;
import com.shoppingmall.service.WishlistService;
import com.shoppingmall.util.MoneySplit;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistServiceImpl implements WishlistService {

    private final OwnershipGuard guard;
    private final WishlistRepository wishlists;
    private final WishlistItemRepository items;
    private final ProductRepository products;
    private final CartService cartService;
    private final Clock clock;

    public WishlistServiceImpl(
            OwnershipGuard guard,
            WishlistRepository wishlists,
            WishlistItemRepository items,
            ProductRepository products,
            CartService cartService,
            Clock clock) {
        this.guard = guard;
        this.wishlists = wishlists;
        this.items = items;
        this.products = products;
        this.cartService = cartService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rows.WishlistRow> view(long userId) {
        Wishlist wishlist = wishlist(guard.requireCustomer(userId));
        return wishlist.getItems().stream()
                .sorted(Comparator.comparing(WishlistItem::getId))
                .map(this::row)
                .toList();
    }

    @Override
    @Transactional
    public void add(long userId, long productId) {
        Customer customer = guard.requireCustomer(userId);
        Product product = products.findById(productId)
                .orElseThrow(() -> new BusinessException("That product is not available.", "/customer/wishlist"));
        if (!ProductServiceImpl.sellable(product)) {
            throw new BusinessException("That product is not available.", "/customer/wishlist");
        }
        Wishlist wishlist = wishlist(customer);
        if (items.existsByWishlistIdAndProductId(wishlist.getId(), productId)) {
            throw new BusinessException("That product is already on your wishlist.", "/customer/wishlist");
        }
        WishlistItem item = new WishlistItem();
        item.setWishlist(wishlist);
        item.setProduct(product);
        item.setCreatedAt(LocalDateTime.now(clock));
        items.save(item);
    }

    @Override
    @Transactional
    public void remove(long userId, long itemId) {
        WishlistItem item = items.findByIdAndWishlistCustomerUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Wishlist item was not found."));
        items.delete(item);
    }

    @Override
    @Transactional
    public void moveToCart(long userId, long itemId) {
        WishlistItem item = items.findByIdAndWishlistCustomerUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Wishlist item was not found."));
        cartService.add(userId, item.getProduct().getId(), 1);
        items.delete(item);
    }

    private Wishlist wishlist(Customer customer) {
        return wishlists.findByCustomerId(customer.getId())
                .orElseThrow(() -> new NotFoundException("Wishlist was not found."));
    }

    private Rows.WishlistRow row(WishlistItem item) {
        Rows.WishlistRow row = new Rows.WishlistRow();
        row.setId(item.getId());
        row.setProductId(item.getProduct().getId());
        row.setProductName(item.getProduct().getName());
        row.setPriceLabel(MoneySplit.rupees(item.getProduct().getUnitPrice()));
        row.setImageUrl(ProductServiceImpl.imageUrl(item.getProduct()));
        return row;
    }
}
