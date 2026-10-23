package com.shoppingmall.service.impl;

import com.shoppingmall.dto.ProductCard;
import com.shoppingmall.dto.ProductForm;
import com.shoppingmall.entity.Category;
import com.shoppingmall.entity.Inventory;
import com.shoppingmall.entity.Product;
import com.shoppingmall.entity.Shop;
import com.shoppingmall.enums.ProductStatus;
import com.shoppingmall.enums.ShopStatus;
import com.shoppingmall.enums.StockMovementType;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.CategoryRepository;
import com.shoppingmall.repository.InventoryRepository;
import com.shoppingmall.repository.ProductRepository;
import com.shoppingmall.repository.ShopRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.AuditService;
import com.shoppingmall.service.ProductService;
import com.shoppingmall.util.Money;
import com.shoppingmall.util.MoneySplit;
import com.shoppingmall.util.Pages;
import com.shoppingmall.util.Slugger;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository products;
    private final ShopRepository shops;
    private final CategoryRepository categories;
    private final InventoryRepository inventories;
    private final OwnershipGuard guard;
    private final AuditService audit;
    private final ImageFiles images;
    private final StockLedger ledger;

    public ProductServiceImpl(
            ProductRepository products,
            ShopRepository shops,
            CategoryRepository categories,
            InventoryRepository inventories,
            OwnershipGuard guard,
            AuditService audit,
            ImageFiles images,
            StockLedger ledger) {
        this.products = products;
        this.shops = shops;
        this.categories = categories;
        this.inventories = inventories;
        this.guard = guard;
        this.audit = audit;
        this.images = images;
        this.ledger = ledger;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCard> featured() {
        return products.featured(PageRequest.of(0, 8)).stream().map(this::card).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCard> searchPublic(String query, String categorySlug, int page) {
        String q = query == null || query.isBlank() ? "" : "%" + query.trim().toLowerCase() + "%";
        String slug = categorySlug == null ? "" : categorySlug;
        return products.searchPublic(q, slug, Pages.of(page)).map(this::card);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCard getPublic(long id) {
        Product product = products.findById(id).filter(ProductServiceImpl::sellable)
                .orElseThrow(() -> new NotFoundException("Product was not found."));
        return card(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCard> listForOwner(long userId, int page) {
        List<Long> shopIds = guard.requireOwner(userId).getId() == null
                ? List.of()
                : shops.findByOwnerId(guard.requireOwner(userId).getId()).stream().map(Shop::getId).toList();
        if (shopIds.isEmpty()) {
            return Page.empty(Pages.of(page));
        }
        return products.findByShopIdIn(shopIds, Pages.of(page)).map(this::card);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCard> listAll(int page) {
        return products.findAll(Pages.of(page)).map(this::card);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCard getForOwner(long userId, long id) {
        return card(guard.requireOwnProduct(userId, id));
    }

    @Override
    @Transactional
    public long create(long userId, ProductForm form) {
        Shop shop = guard.requireOwnShop(userId, form.getShopId());
        if (shop.getStatus() == ShopStatus.CLOSED) {
            throw new BusinessException("This shop is closed.", "/owner/products/new");
        }
        Category category = activeCategory(form.getCategoryId());
        if (products.existsByShopIdAndSkuIgnoreCase(shop.getId(), form.getSku().trim())) {
            throw new BusinessException("That SKU is already used in this shop.", "/owner/products/new");
        }
        int opening = form.getOpeningQuantity() == null ? 0 : form.getOpeningQuantity();
        if (opening < 0) {
            throw new BusinessException("Opening quantity cannot be negative.", "/owner/products/new");
        }
        Product product = new Product();
        product.setShop(shop);
        product.setCategory(category);
        product.setName(form.getName().trim());
        product.setSku(form.getSku().trim());
        product.setSlug(uniqueSlug(shop.getId(), Slugger.slug(form.getName())));
        product.setDescription(form.getDescription());
        product.setUnitPrice(Money.scale(form.getUnitPrice()));
        product.setCompareAtPrice(form.getCompareAtPrice() == null ? null : Money.scale(form.getCompareAtPrice()));
        product.setTaxRatePercent(Money.scale(form.getTaxRatePercent()));
        product.setStatus(form.getStatus() == null ? ProductStatus.DRAFT : form.getStatus());
        products.save(product);
        storeImage(product, form, "/owner/products/new");
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantityOnHand(0);
        inventory.setQuantityReserved(0);
        inventories.save(inventory);
        product.setInventory(inventory);
        if (opening > 0) {
            ledger.apply(inventory, opening, 0, StockMovementType.OPENING, null, null, "Opening stock", guard.requireUser(userId));
        } else {
            ledger.apply(inventory, 0, 0, StockMovementType.OPENING, null, null, "Opening stock", guard.requireUser(userId));
        }
        return product.getId();
    }

    @Override
    @Transactional
    public void update(long userId, long id, ProductForm form) {
        Product product = guard.requireOwnProduct(userId, id);
        if (product.getShop().getStatus() == ShopStatus.CLOSED) {
            throw new BusinessException("This shop is closed.", "/owner/products/" + id + "/edit");
        }
        product.setCategory(activeCategory(form.getCategoryId()));
        product.setName(form.getName().trim());
        product.setDescription(form.getDescription());
        product.setUnitPrice(Money.scale(form.getUnitPrice()));
        product.setCompareAtPrice(form.getCompareAtPrice() == null ? null : Money.scale(form.getCompareAtPrice()));
        product.setTaxRatePercent(Money.scale(form.getTaxRatePercent()));
        if (form.getStatus() != null) {
            product.setStatus(form.getStatus());
        }
        storeImage(product, form, "/owner/products/" + id + "/edit");
    }

    @Override
    @Transactional
    public void archive(long adminId, long id) {
        Product product = products.findById(id).orElseThrow(() -> new NotFoundException("Product was not found."));
        product.setStatus(ProductStatus.ARCHIVED);
        audit.record(adminId, "PRODUCT_ARCHIVED", "products", id, "Archived " + product.getName() + ".");
    }

    private void storeImage(Product product, ProductForm form, String redirect) {
        if (form.getImage() == null || form.getImage().isEmpty()) {
            return;
        }
        try {
            product.setImagePath(images.store("products", product.getId(), form.getImage().getBytes()));
        } catch (Exception exception) {
            String message = exception instanceof BusinessException business
                    ? business.getMessage()
                    : "Upload a JPEG, PNG, or WEBP image within the size limit.";
            throw new BusinessException(message, redirect);
        }
    }

    private Category activeCategory(Long categoryId) {
        Category category = categories.findById(categoryId == null ? -1 : categoryId)
                .orElseThrow(() -> new BusinessException("Choose an active category.", "/owner/products/new"));
        if (!category.isActive()) {
            throw new BusinessException("Choose an active category.", "/owner/products/new");
        }
        return category;
    }

    private String uniqueSlug(Long shopId, String base) {
        String slug = base;
        int suffix = 2;
        while (products.existsByShopIdAndSlug(shopId, slug)) {
            slug = base + "-" + suffix;
            suffix++;
        }
        return slug.length() > 200 ? slug.substring(0, 200) : slug;
    }

    private ProductCard card(Product product) {
        ProductCard card = new ProductCard();
        card.setId(product.getId());
        card.setShopId(product.getShop().getId());
        card.setCategoryId(product.getCategory().getId());
        card.setName(product.getName());
        card.setSlug(product.getSlug());
        card.setSku(product.getSku());
        card.setDescription(product.getDescription());
        card.setUnitPrice(product.getUnitPrice());
        card.setCompareAtPrice(product.getCompareAtPrice());
        card.setTaxRatePercent(product.getTaxRatePercent());
        card.setStatus(product.getStatus().name());
        card.setImageUrl(imageUrl(product));
        card.setShopName(product.getShop().getName());
        card.setShopSlug(product.getShop().getSlug());
        card.setCategoryName(product.getCategory().getName());
        card.setStock(product.getInventory() == null ? 0 : product.getInventory().getQuantityOnHand());
        card.setPriceLabel(MoneySplit.rupees(product.getUnitPrice()));
        return card;
    }

    static boolean sellable(Product product) {
        return product.getStatus() == ProductStatus.ACTIVE && product.getShop().getStatus() == ShopStatus.ACTIVE;
    }

    static String imageUrl(Product product) {
        if (product.getImagePath() == null || product.getImagePath().isBlank()) {
            return "/images/product.svg";
        }
        String file = product.getImagePath().substring(product.getImagePath().lastIndexOf('/') + 1);
        return "/media/products/" + product.getId() + "/" + file;
    }
}
