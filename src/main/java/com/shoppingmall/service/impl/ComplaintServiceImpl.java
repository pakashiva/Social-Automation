package com.shoppingmall.service.impl;

import com.shoppingmall.dto.ComplaintForm;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.Complaint;
import com.shoppingmall.entity.MallOrder;
import com.shoppingmall.entity.Shop;
import com.shoppingmall.entity.User;
import com.shoppingmall.enums.ComplaintStatus;
import com.shoppingmall.enums.NotificationType;
import com.shoppingmall.enums.RoleName;
import com.shoppingmall.enums.UserStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.ComplaintRepository;
import com.shoppingmall.repository.MallOrderRepository;
import com.shoppingmall.repository.ShopRepository;
import com.shoppingmall.repository.UserRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.AuditService;
import com.shoppingmall.service.ComplaintService;
import com.shoppingmall.service.NotificationService;
import com.shoppingmall.service.NumberSequenceService;
import com.shoppingmall.util.Pages;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaints;
    private final MallOrderRepository orders;
    private final ShopRepository shops;
    private final UserRepository users;
    private final OwnershipGuard guard;
    private final NumberSequenceService sequences;
    private final NotificationService notifications;
    private final AuditService audit;
    private final Clock clock;

    public ComplaintServiceImpl(
            ComplaintRepository complaints,
            MallOrderRepository orders,
            ShopRepository shops,
            UserRepository users,
            OwnershipGuard guard,
            NumberSequenceService sequences,
            NotificationService notifications,
            AuditService audit,
            Clock clock) {
        this.complaints = complaints;
        this.orders = orders;
        this.shops = shops;
        this.users = users;
        this.guard = guard;
        this.sequences = sequences;
        this.notifications = notifications;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.ComplaintRow> listForCustomer(long userId, int page) {
        return complaints.findByCustomerIdOrderByCreatedAtDesc(guard.requireCustomer(userId).getId(), Pages.of(page)).map(this::row);
    }

    @Override
    @Transactional(readOnly = true)
    public Rows.ComplaintRow getForCustomer(long userId, String complaintNumber) {
        Complaint complaint = complaints.findByComplaintNumber(complaintNumber)
                .filter(found -> found.getCustomer().getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Complaint was not found."));
        return row(complaint);
    }

    @Override
    @Transactional
    public void create(long userId, ComplaintForm form) {
        var customer = guard.requireCustomer(userId);
        Complaint complaint = new Complaint();
        complaint.setComplaintNumber(sequences.next("COMPLAINT"));
        complaint.setCustomer(customer);
        complaint.setCategory(form.getCategory());
        complaint.setSubject(form.getSubject().trim());
        complaint.setDescription(form.getDescription().trim());
        complaint.setStatus(ComplaintStatus.OPEN);
        if (form.getOrderNumber() != null && !form.getOrderNumber().isBlank()) {
            MallOrder order = orders.findByOrderNumber(form.getOrderNumber().trim())
                    .filter(found -> found.getCustomer().getId().equals(customer.getId()))
                    .orElseThrow(() -> new BusinessException(
                            "That order was not found on your account.", "/customer/complaints/new"));
            complaint.setOrder(order);
        }
        if (form.getShopId() != null) {
            Shop shop = shops.findById(form.getShopId())
                    .orElseThrow(() -> new BusinessException("That shop was not found.", "/customer/complaints/new"));
            complaint.setShop(shop);
        }
        complaints.save(complaint);
        String link = "/admin/complaints/" + complaint.getComplaintNumber();
        for (User admin : users.findByStatusAndRole(UserStatus.ACTIVE, RoleName.ADMIN)) {
            notifications.notifyUser(admin.getId(), NotificationType.COMPLAINT, "New complaint",
                    complaint.getSubject(), link);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.ComplaintRow> listAll(int page) {
        return complaints.findAllByOrderByCreatedAtDesc(Pages.of(page)).map(this::row);
    }

    @Override
    @Transactional(readOnly = true)
    public Rows.ComplaintRow get(String complaintNumber) {
        return row(load(complaintNumber));
    }

    @Override
    @Transactional
    public void assign(String complaintNumber, long assigneeId) {
        Complaint complaint = load(complaintNumber);
        if (closed(complaint)) {
            throw new BusinessException("This complaint is already closed.", "/admin/complaints/" + complaintNumber);
        }
        User assignee = users.findById(assigneeId).orElse(null);
        if (assignee == null
                || assignee.getStatus() != UserStatus.ACTIVE
                || assignee.getUserRole() == null
                || assignee.getUserRole().getRole().getName() != RoleName.ADMIN) {
            throw new BusinessException("Assign the complaint to an active admin.", "/admin/complaints/" + complaintNumber);
        }
        complaint.setAssignedTo(assignee);
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaints.save(complaint);
    }

    @Override
    @Transactional
    public void resolve(long adminId, String complaintNumber, String resolutionNote) {
        close(adminId, complaintNumber, resolutionNote, ComplaintStatus.RESOLVED, "COMPLAINT_RESOLVED");
    }

    @Override
    @Transactional
    public void reject(long adminId, String complaintNumber, String resolutionNote) {
        close(adminId, complaintNumber, resolutionNote, ComplaintStatus.REJECTED, "COMPLAINT_REJECTED");
    }

    private void close(long adminId, String complaintNumber, String resolutionNote, ComplaintStatus status, String action) {
        Complaint complaint = load(complaintNumber);
        if (closed(complaint)) {
            throw new BusinessException("This complaint is already closed.", "/admin/complaints/" + complaintNumber);
        }
        if (resolutionNote == null || resolutionNote.trim().length() < 10 || resolutionNote.trim().length() > 1000) {
            throw new BusinessException("Write at least 10 characters.", "/admin/complaints/" + complaintNumber);
        }
        complaint.setStatus(status);
        complaint.setResolutionNote(resolutionNote.trim());
        complaint.setResolvedAt(LocalDateTime.now(clock));
        complaint.setAssignedTo(guard.requireUser(adminId));
        complaints.save(complaint);
        audit.record(adminId, action, "complaints", complaint.getId(), complaint.getComplaintNumber());
        notifications.notifyUser(
                complaint.getCustomer().getUser().getId(),
                NotificationType.COMPLAINT,
                "Complaint " + status.name().toLowerCase(),
                complaint.getSubject(),
                "/customer/complaints/" + complaint.getComplaintNumber());
    }

    private Complaint load(String complaintNumber) {
        return complaints.findByComplaintNumber(complaintNumber)
                .orElseThrow(() -> new NotFoundException("Complaint was not found."));
    }

    private static boolean closed(Complaint complaint) {
        return complaint.getStatus() == ComplaintStatus.RESOLVED || complaint.getStatus() == ComplaintStatus.REJECTED;
    }

    private Rows.ComplaintRow row(Complaint complaint) {
        Rows.ComplaintRow row = new Rows.ComplaintRow();
        row.setComplaintNumber(complaint.getComplaintNumber());
        row.setCategory(complaint.getCategory().name());
        row.setSubject(complaint.getSubject());
        row.setDescription(complaint.getDescription());
        row.setStatus(complaint.getStatus().name());
        row.setCustomerName(complaint.getCustomer().getUser().getFullName());
        row.setResolutionNote(complaint.getResolutionNote());
        row.setShopName(complaint.getShop() == null ? "" : complaint.getShop().getName());
        row.setOrderNumber(complaint.getOrder() == null ? "" : complaint.getOrder().getOrderNumber());
        return row;
    }
}
