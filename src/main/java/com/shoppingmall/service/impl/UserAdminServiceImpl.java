package com.shoppingmall.service.impl;

import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.Customer;
import com.shoppingmall.entity.ShopOwner;
import com.shoppingmall.entity.User;
import com.shoppingmall.enums.ApprovalStatus;
import com.shoppingmall.enums.RoleName;
import com.shoppingmall.enums.UserStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.CustomerRepository;
import com.shoppingmall.repository.RoleRepository;
import com.shoppingmall.repository.ShopOwnerRepository;
import com.shoppingmall.repository.UserRepository;
import com.shoppingmall.security.PermissionMatrix;
import com.shoppingmall.service.AuditService;
import com.shoppingmall.service.UserAdminService;
import com.shoppingmall.util.Pages;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository users;
    private final CustomerRepository customers;
    private final ShopOwnerRepository owners;
    private final RoleRepository roles;
    private final AuditService audit;

    public UserAdminServiceImpl(
            UserRepository users,
            CustomerRepository customers,
            ShopOwnerRepository owners,
            RoleRepository roles,
            AuditService audit) {
        this.users = users;
        this.customers = customers;
        this.owners = owners;
        this.roles = roles;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.UserRow> search(String query, int page) {
        Page<User> found = query == null || query.isBlank()
                ? users.findAll(Pages.of(page))
                : users.search("%" + query.trim().toLowerCase() + "%", Pages.of(page));
        return found.map(this::row);
    }

    @Override
    @Transactional(readOnly = true)
    public Rows.UserRow get(long id) {
        return row(users.findById(id).orElseThrow(() -> new NotFoundException("User was not found.")));
    }

    @Override
    @Transactional
    public void changeStatus(long adminId, long userId, UserStatus status) {
        if (status != UserStatus.ACTIVE && status != UserStatus.INACTIVE) {
            throw new BusinessException("Choose active or inactive.", "/admin/users/" + userId);
        }
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User was not found."));
        RoleName role = user.getUserRole().getRole().getName();
        if (status == UserStatus.INACTIVE && role == RoleName.ADMIN && user.getStatus() == UserStatus.ACTIVE
                && users.countActiveAdmins() <= 1) {
            throw new BusinessException("The last active admin cannot be deactivated.", "/admin/users/" + userId);
        }
        if (status == UserStatus.ACTIVE && role == RoleName.SHOP_OWNER) {
            ShopOwner owner = owners.findByUserId(userId).orElse(null);
            if (owner == null || owner.getApprovalStatus() != ApprovalStatus.APPROVED) {
                throw new BusinessException("Approve the shop owner from the shop screen.", "/admin/users/" + userId);
            }
        }
        user.setStatus(status);
        audit.record(adminId, "USER_STATUS_CHANGED", "users", userId, "User status set to " + status + ".");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rows.RoleCount> roleSummary() {
        List<Rows.RoleCount> rows = new ArrayList<>();
        rows.add(count(RoleName.ADMIN, "Mall administrator", PermissionMatrix.admin()));
        rows.add(count(RoleName.SHOP_OWNER, "Approved shop owner", PermissionMatrix.shopOwner()));
        rows.add(count(RoleName.CUSTOMER, "Shopper", PermissionMatrix.customer()));
        return rows;
    }

    private Rows.RoleCount count(RoleName name, String fallback, List<String> permissions) {
        Rows.RoleCount row = new Rows.RoleCount();
        row.setName(name.name());
        row.setDescription(roles.findByName(name).map(role -> role.getDescription()).orElse(fallback));
        row.setUsers(users.countByRole(name));
        row.setPermissions(permissions);
        return row;
    }

    private Rows.UserRow row(User user) {
        Rows.UserRow row = new Rows.UserRow();
        row.setId(user.getId());
        row.setEmail(user.getEmail());
        row.setMobile(user.getMobile());
        row.setFullName(user.getFullName());
        row.setStatus(user.getStatus().name());
        RoleName role = user.getUserRole().getRole().getName();
        row.setRole(role.name());
        if (role == RoleName.SHOP_OWNER) {
            owners.findByUserId(user.getId()).ifPresent(owner -> {
                row.setApprovalStatus(owner.getApprovalStatus().name());
                row.setBusinessName(owner.getBusinessName());
                row.setRejectionReason(owner.getRejectionReason());
            });
        }
        if (role == RoleName.CUSTOMER) {
            customers.findByUserId(user.getId()).ifPresent(customer -> row.setAddress(address(customer)));
        }
        return row;
    }

    private static String address(Customer customer) {
        return join(customer.getAddressLine1(), customer.getAddressLine2(), customer.getCity(),
                customer.getState(), customer.getPostalCode(), customer.getCountry());
    }

    static String join(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(part.trim());
        }
        return builder.toString();
    }
}
