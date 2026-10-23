package com.shoppingmall.service.impl;

import com.shoppingmall.dto.CouponForm;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.Category;
import com.shoppingmall.entity.Coupon;
import com.shoppingmall.entity.Shop;
import com.shoppingmall.enums.CouponScope;
import com.shoppingmall.enums.DiscountType;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.CategoryRepository;
import com.shoppingmall.repository.CouponRepository;
import com.shoppingmall.repository.ShopRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.AuditService;
import com.shoppingmall.service.CouponService;
import com.shoppingmall.util.Money;
import com.shoppingmall.util.MoneySplit;
import com.shoppingmall.util.Pages;
import com.shoppingmall.util.TimeDisplay;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository coupons;
    private final ShopRepository shops;
    private final CategoryRepository categories;
    private final OwnershipGuard guard;
    private final AuditService audit;
    private final TimeDisplay timeDisplay;
    private final Clock clock;

    public CouponServiceImpl(
            CouponRepository coupons,
            ShopRepository shops,
            CategoryRepository categories,
            OwnershipGuard guard,
            AuditService audit,
            TimeDisplay timeDisplay,
            Clock clock) {
        this.coupons = coupons;
        this.shops = shops;
        this.categories = categories;
        this.guard = guard;
        this.audit = audit;
        this.timeDisplay = timeDisplay;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rows.CouponRow> listPublic() {
        return coupons.findPublic(LocalDateTime.now(clock)).stream().map(coupon -> row(coupon, false)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.CouponRow> listForOwner(long userId, int page) {
        List<Long> shopIds = shops.findByOwnerId(guard.requireOwner(userId).getId()).stream().map(Shop::getId).toList();
        if (shopIds.isEmpty()) {
            return Page.empty(Pages.of(page));
        }
        return coupons.findByShopIdIn(shopIds, Pages.of(page)).map(coupon -> row(coupon, true));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.CouponRow> listAll(int page) {
        return coupons.findAllByOrderByCodeAsc(Pages.of(page)).map(coupon -> row(coupon, true));
    }

    @Override
    @Transactional(readOnly = true)
    public Rows.CouponChoices formData() {
        List<Rows.NamedOption> shopOptions = shops.findAll().stream().map(shop -> option(shop.getId(), shop.getName())).toList();
        List<Rows.NamedOption> categoryOptions = categories.findAll().stream()
                .map(category -> option(category.getId(), category.getName()))
                .toList();
        return new Rows.CouponChoices(shopOptions, categoryOptions);
    }

    @Override
    @Transactional
    public void createShopCoupon(long userId, CouponForm form) {
        if (form.getShopId() == null) {
            throw new BusinessException("Choose a shop.", "/owner/offers/new");
        }
        Shop shop = guard.requireOwnShop(userId, form.getShopId());
        form.setScope(CouponScope.SHOP);
        form.setCategoryId(null);
        form.setShopId(shop.getId());
        save(form, guard.requireUser(userId).getId(), "/owner/offers/new");
    }

    @Override
    @Transactional
    public void deactivateOwn(long userId, long id) {
        Coupon coupon = guard.requireOwnCoupon(userId, id);
        coupon.setActive(false);
        coupons.save(coupon);
    }

    @Override
    @Transactional
    public void create(long adminId, CouponForm form) {
        guard.requireUser(adminId);
        save(form, adminId, "/admin/coupons/new");
    }

    @Override
    @Transactional
    public void deactivate(long adminId, long id) {
        Coupon coupon = coupons.findById(id).orElseThrow(() -> new NotFoundException("Coupon was not found."));
        coupon.setActive(false);
        coupons.save(coupon);
        audit.record(adminId, "COUPON_DEACTIVATED", "coupons", coupon.getId(), "Deactivated " + coupon.getCode());
    }

    private void save(CouponForm form, long userId, String redirect) {
        String code = form.getCode() == null ? "" : form.getCode().trim().toUpperCase();
        if (code.isBlank() || code.length() > 40) {
            throw new BusinessException("Enter a coupon code.", redirect);
        }
        if (coupons.existsByCode(code)) {
            throw new BusinessException("That coupon code is already used.", redirect);
        }
        if (form.getDiscountType() == DiscountType.PERCENT
                && form.getDiscountValue() != null
                && form.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("Percent discount cannot be above 100.", redirect);
        }
        if (form.getDiscountValue() == null || form.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Discount must be greater than zero.", redirect);
        }
        if (form.getStartsAt() == null || form.getEndsAt() == null || !form.getEndsAt().isAfter(form.getStartsAt())) {
            throw new BusinessException("The end time must be after the start time.", redirect);
        }
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDescription(form.getDescription().trim());
        coupon.setDiscountType(form.getDiscountType());
        coupon.setDiscountValue(Money.scale(form.getDiscountValue()));
        coupon.setMinOrderAmount(Money.scale(form.getMinOrderAmount() == null ? BigDecimal.ZERO : form.getMinOrderAmount()));
        coupon.setMaxDiscountAmount(form.getMaxDiscountAmount() == null ? null : Money.scale(form.getMaxDiscountAmount()));
        coupon.setUsageLimit(form.getUsageLimit());
        coupon.setUsageCount(0);
        coupon.setPerCustomerLimit(form.getPerCustomerLimit() == null ? 1 : form.getPerCustomerLimit());
        coupon.setStartsAt(form.getStartsAt());
        coupon.setEndsAt(form.getEndsAt());
        coupon.setActive(true);
        coupon.setCreatedBy(guard.requireUser(userId));
        applyScope(coupon, form, redirect);
        coupons.save(coupon);
    }

    private void applyScope(Coupon coupon, CouponForm form, String redirect) {
        CouponScope scope = form.getScope() == null ? CouponScope.MALL : form.getScope();
        coupon.setScope(scope);
        if (scope == CouponScope.MALL) {
            coupon.setShop(null);
            coupon.setCategory(null);
            return;
        }
        if (scope == CouponScope.SHOP) {
            if (form.getShopId() == null) {
                throw new BusinessException("Choose a shop.", redirect);
            }
            Shop shop = shops.findById(form.getShopId()).orElseThrow(() -> new BusinessException("Choose a shop.", redirect));
            coupon.setShop(shop);
            coupon.setCategory(null);
            return;
        }
        if (form.getCategoryId() == null) {
            throw new BusinessException("Choose a category.", redirect);
        }
        Category category = categories.findById(form.getCategoryId())
                .orElseThrow(() -> new BusinessException("Choose a category.", redirect));
        coupon.setCategory(category);
        coupon.setShop(null);
    }

    private Rows.CouponRow row(Coupon coupon, boolean management) {
        Rows.CouponRow row = new Rows.CouponRow();
        row.setId(coupon.getId());
        row.setCode(coupon.getCode());
        row.setDescription(coupon.getDescription());
        row.setDiscountLabel(label(coupon));
        row.setMinimum(MoneySplit.rupees(coupon.getMinOrderAmount()));
        row.setEndsAt(timeDisplay.format(coupon.getEndsAt()));
        row.setScopeLabel(scopeLabel(coupon));
        row.setActive(coupon.isActive());
        row.setShopName(coupon.getShop() == null ? "" : coupon.getShop().getName());
        if (!management) {
            row.setShopName(row.getScopeLabel());
        }
        return row;
    }

    private static String label(Coupon coupon) {
        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            return coupon.getDiscountValue().stripTrailingZeros().toPlainString() + "%";
        }
        return MoneySplit.rupees(coupon.getDiscountValue());
    }

    private static String scopeLabel(Coupon coupon) {
        if (coupon.getScope() == CouponScope.SHOP && coupon.getShop() != null) {
            return coupon.getShop().getName();
        }
        if (coupon.getScope() == CouponScope.CATEGORY && coupon.getCategory() != null) {
            return coupon.getCategory().getName();
        }
        return "Whole mall";
    }

    private static Rows.NamedOption option(Long id, String name) {
        Rows.NamedOption option = new Rows.NamedOption();
        option.setId(id);
        option.setName(name);
        return option;
    }
}
