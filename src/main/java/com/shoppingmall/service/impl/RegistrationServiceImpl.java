package com.shoppingmall.service.impl;

import com.shoppingmall.dto.RegistrationForm;
import com.shoppingmall.validation.FieldRules;
import com.shoppingmall.entity.Cart;
import com.shoppingmall.entity.Customer;
import com.shoppingmall.entity.Role;
import com.shoppingmall.entity.Shop;
import com.shoppingmall.entity.ShopOwner;
import com.shoppingmall.entity.User;
import com.shoppingmall.entity.UserRole;
import com.shoppingmall.entity.Wishlist;
import com.shoppingmall.enums.ApprovalStatus;
import com.shoppingmall.enums.CartStatus;
import com.shoppingmall.enums.RoleName;
import com.shoppingmall.enums.ShopStatus;
import com.shoppingmall.enums.UserStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.DuplicateAccountException;
import com.shoppingmall.repository.CartRepository;
import com.shoppingmall.repository.CustomerRepository;
import com.shoppingmall.repository.RoleRepository;
import com.shoppingmall.repository.ShopOwnerRepository;
import com.shoppingmall.repository.ShopRepository;
import com.shoppingmall.repository.UserRepository;
import com.shoppingmall.repository.UserRoleRepository;
import com.shoppingmall.repository.WishlistRepository;
import com.shoppingmall.service.RegistrationService;
import com.shoppingmall.util.Slugger;
import com.shoppingmall.validation.GstNumber;
import com.shoppingmall.validation.MobileNumber;
import com.shoppingmall.validation.PasswordRules;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final CustomerRepository customers;
    private final ShopOwnerRepository owners;
    private final ShopRepository shops;
    private final CartRepository carts;
    private final WishlistRepository wishlists;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public RegistrationServiceImpl(
            UserRepository users,
            RoleRepository roles,
            UserRoleRepository userRoles,
            CustomerRepository customers,
            ShopOwnerRepository owners,
            ShopRepository shops,
            CartRepository carts,
            WishlistRepository wishlists,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
        this.customers = customers;
        this.owners = owners;
        this.shops = shops;
        this.carts = carts;
        this.wishlists = wishlists;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void register(RegistrationForm form) {
        if (!FieldRules.password(form.getPassword()) || !form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Use 8 to 72 characters with at least one letter and one digit.", "/register");
        }
        String email = form.getEmail().trim().toLowerCase();
        String mobile = form.getMobile().trim();
        if (users.existsByEmail(email)) {
            throw new DuplicateAccountException("email", "An account with this email already exists.");
        }
        if (users.existsByMobile(mobile)) {
            throw new DuplicateAccountException("mobile", "An account with this mobile number already exists.");
        }
        boolean ownerAccount = "SHOP_OWNER".equals(form.getAccountType());
        if (!ownerAccount && !"CUSTOMER".equals(form.getAccountType())) {
            throw new BusinessException("Choose customer or shop owner.", "/register");
        }
        User user = new User();
        user.setEmail(email);
        user.setMobile(mobile);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setFullName(form.getFullName().trim());
        user.setStatus(ownerAccount ? UserStatus.PENDING : UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        users.save(user);
        Role role = roles.findByName(ownerAccount ? RoleName.SHOP_OWNER : RoleName.CUSTOMER)
                .orElseThrow(() -> new BusinessException("Registration is not available.", "/register"));
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedAt(LocalDateTime.now(clock));
        userRoles.save(userRole);
        if (!ownerAccount) {
            Customer customer = new Customer();
            customer.setUser(user);
            customer.setCountry("India");
            customers.save(customer);
            Cart cart = new Cart();
            cart.setCustomer(customer);
            cart.setStatus(CartStatus.OPEN);
            carts.save(cart);
            Wishlist wishlist = new Wishlist();
            wishlist.setCustomer(customer);
            wishlists.save(wishlist);
            return;
        }
        if (form.getOpeningTime() == null || form.getClosingTime() == null
                || !form.getOpeningTime().isBefore(form.getClosingTime())) {
            throw new BusinessException("Closing time must be after opening time.", "/register");
        }
        if (!FieldRules.mobile(form.getShopPhone())) {
            throw new BusinessException("Enter a 10-digit mobile number starting with 6, 7, 8, or 9.", "/register");
        }
        String gst = blank(form.getGstNumber()) ? null : form.getGstNumber().trim().toUpperCase();
        if (gst != null && !FieldRules.gst(gst)) {
            throw new BusinessException("Enter a valid GST number or leave it blank.", "/register");
        }
        if (gst != null && owners.existsByGstNumber(gst)) {
            throw new DuplicateAccountException("gstNumber", "That GST number is already registered.");
        }
        if (shops.existsByFloorLabelAndUnitNumber(form.getFloorLabel().trim(), form.getUnitNumber().trim())) {
            throw new DuplicateAccountException("unitNumber", "That floor and unit is already taken.");
        }
        ShopOwner owner = new ShopOwner();
        owner.setUser(user);
        owner.setBusinessName(form.getBusinessName().trim());
        owner.setGstNumber(gst);
        owner.setApprovalStatus(ApprovalStatus.PENDING);
        owners.save(owner);
        Shop shop = new Shop();
        shop.setOwner(owner);
        shop.setName(form.getShopName().trim());
        shop.setSlug(uniqueSlug(Slugger.slug(form.getShopName(), form.getUnitNumber())));
        shop.setFloorLabel(form.getFloorLabel().trim());
        shop.setUnitNumber(form.getUnitNumber().trim());
        shop.setContactPhone(form.getShopPhone().trim());
        shop.setContactEmail(email);
        shop.setOpeningTime(form.getOpeningTime());
        shop.setClosingTime(form.getClosingTime());
        shop.setStatus(ShopStatus.DRAFT);
        shops.save(shop);
    }

    private String uniqueSlug(String base) {
        String slug = base;
        int suffix = 2;
        while (shops.existsBySlug(slug)) {
            slug = base + "-" + suffix;
            suffix++;
        }
        return slug.length() > 180 ? slug.substring(0, 180) : slug;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
