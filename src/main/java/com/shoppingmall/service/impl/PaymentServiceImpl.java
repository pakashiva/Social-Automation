package com.shoppingmall.service.impl;

import com.shoppingmall.dto.InvoiceView;
import com.shoppingmall.dto.OrderView;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.Inventory;
import com.shoppingmall.entity.Invoice;
import com.shoppingmall.entity.MallOrder;
import com.shoppingmall.entity.OrderItem;
import com.shoppingmall.entity.OrderStatusHistory;
import com.shoppingmall.entity.Payment;
import com.shoppingmall.enums.NotificationType;
import com.shoppingmall.enums.OrderStatus;
import com.shoppingmall.enums.PaymentStatus;
import com.shoppingmall.enums.SimulatedOutcome;
import com.shoppingmall.enums.StockMovementType;
import com.shoppingmall.exception.BusinessException;
import java.util.List;
import com.shoppingmall.repository.InventoryRepository;
import com.shoppingmall.repository.InvoiceRepository;
import com.shoppingmall.repository.OrderStatusHistoryRepository;
import com.shoppingmall.repository.PaymentRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.NotificationService;
import com.shoppingmall.service.NumberSequenceService;
import com.shoppingmall.service.PaymentService;
import com.shoppingmall.util.Pages;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PaymentServiceImpl implements PaymentService {

    static final String DECLINED = "Simulated payment was declined. No charge was made. Place a new order if you still want these items.";

    private final OwnershipGuard guard;
    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final InventoryRepository inventories;
    private final OrderStatusHistoryRepository history;
    private final NumberSequenceService sequences;
    private final StockLedger ledger;
    private final NotificationService notifications;
    private final Clock clock;
    private final EntityManager entityManager;
    private final TransactionTemplate transactions;

    public PaymentServiceImpl(
            OwnershipGuard guard,
            PaymentRepository payments,
            InvoiceRepository invoices,
            InventoryRepository inventories,
            OrderStatusHistoryRepository history,
            NumberSequenceService sequences,
            StockLedger ledger,
            NotificationService notifications,
            Clock clock,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager) {
        this.guard = guard;
        this.payments = payments;
        this.invoices = invoices;
        this.inventories = inventories;
        this.history = history;
        this.sequences = sequences;
        this.ledger = ledger;
        this.notifications = notifications;
        this.clock = clock;
        this.entityManager = entityManager;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public InvoiceView pay(long userId, String orderNumber, SimulatedOutcome outcome) {
        PayResult result = transactions.execute(status -> apply(userId, orderNumber, outcome));
        if (result.declined()) {
            throw new BusinessException(DECLINED, "/customer/orders/" + orderNumber);
        }
        return result.invoice();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<Rows.PaymentRow> search(int page) {
        return payments.findAllByOrderByIdDesc(Pages.of(page)).map(payment -> {
            Rows.PaymentRow row = new Rows.PaymentRow();
            row.setPaymentNumber(payment.getPaymentNumber());
            row.setOrderNumber(payment.getOrder().getOrderNumber());
            row.setStatus(payment.getStatus().name());
            row.setMethod(payment.getMethod().name());
            row.setAmount(payment.getAmount());
            row.setOutcome(payment.getSimulatedOutcome() == null ? "" : payment.getSimulatedOutcome().name());
            row.setPaidAt(payment.getPaidAt());
            return row;
        });
    }

    private PayResult apply(long userId, String orderNumber, SimulatedOutcome outcome) {
        MallOrder order = guard.requireOwnCustomerOrder(userId, orderNumber);
        Payment payment = payments.findByOrderId(order.getId()).orElseThrow();
        entityManager.refresh(payment, LockModeType.PESSIMISTIC_WRITE);
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return new PayResult(toView(invoices.findByOrderId(order.getId()).orElseThrow()), false);
        }
        if (payment.getStatus() != PaymentStatus.PENDING || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("This order is not waiting for payment.", "/customer/orders/" + orderNumber);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        List<OrderItem> lines = order.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();
        if (outcome == SimulatedOutcome.DECLINED) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Simulated decline");
            payment.setSimulatedOutcome(SimulatedOutcome.DECLINED);
            for (OrderItem line : lines) {
                Inventory inventory = inventories.findByProductId(line.getProduct().getId()).orElseThrow();
                entityManager.refresh(inventory, LockModeType.PESSIMISTIC_WRITE);
                ledger.apply(inventory, line.getQuantity(), -line.getQuantity(), StockMovementType.RELEASE, "ORDER",
                        order.getId(), "Simulated decline", order.getCustomer().getUser());
            }
            transition(order, OrderStatus.PAYMENT_FAILED, userId, "Simulated decline", now);
            return new PayResult(null, true);
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(now);
        payment.setSimulatedOutcome(SimulatedOutcome.SUCCESS);
        payment.setGatewayReference("SIM-" + payment.getPaymentNumber());
        for (OrderItem line : lines) {
            Inventory inventory = inventories.findByProductId(line.getProduct().getId()).orElseThrow();
            entityManager.refresh(inventory, LockModeType.PESSIMISTIC_WRITE);
            ledger.apply(inventory, 0, -line.getQuantity(), StockMovementType.SALE, "ORDER", order.getId(),
                    "Sale " + order.getOrderNumber(), order.getCustomer().getUser());
        }
        transition(order, OrderStatus.CONFIRMED, userId, null, now);
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(sequences.next("INVOICE"));
        invoice.setOrder(order);
        invoice.setPayment(payment);
        invoice.setCustomer(order.getCustomer());
        invoice.setShop(order.getShop());
        invoice.setBilledName(order.getCustomer().getUser().getFullName());
        invoice.setBilledEmail(order.getCustomer().getUser().getEmail());
        invoice.setBilledMobile(order.getCustomer().getUser().getMobile());
        invoice.setBilledAddress(UserAdminServiceImpl.join(
                order.getCustomer().getAddressLine1(),
                order.getCustomer().getAddressLine2(),
                order.getCustomer().getCity(),
                order.getCustomer().getState(),
                order.getCustomer().getPostalCode(),
                order.getCustomer().getCountry()));
        if (invoice.getBilledAddress().isBlank()) {
            invoice.setBilledAddress("India");
        }
        invoice.setSubtotal(order.getSubtotal());
        invoice.setDiscountAmount(order.getDiscountAmount());
        invoice.setTaxAmount(order.getTaxAmount());
        invoice.setTotalAmount(order.getTotalAmount());
        invoice.setCurrencyCode("INR");
        invoice.setIssuedAt(now);
        invoice.setCreatedAt(now);
        invoices.save(invoice);
        notifications.notifyUser(order.getCustomer().getUser().getId(), NotificationType.ORDER_PLACED,
                "Order placed", "Order " + order.getOrderNumber() + " is confirmed.",
                "/customer/orders/" + order.getOrderNumber());
        return new PayResult(toView(invoice), false);
    }

    private void transition(MallOrder order, OrderStatus status, long userId, String note, LocalDateTime now) {
        OrderStatus from = order.getStatus();
        order.setStatus(status);
        OrderStatusHistory row = new OrderStatusHistory();
        row.setOrder(order);
        row.setFromStatus(from);
        row.setToStatus(status);
        row.setChangedBy(guard.requireUser(userId));
        row.setNote(note);
        row.setCreatedAt(now);
        history.save(row);
    }

    private InvoiceView toView(Invoice invoice) {
        InvoiceView view = new InvoiceView();
        view.setInvoiceNumber(invoice.getInvoiceNumber());
        view.setOrderNumber(invoice.getOrder().getOrderNumber());
        view.setShopName(invoice.getShop().getName());
        view.setBilledName(invoice.getBilledName());
        view.setBilledEmail(invoice.getBilledEmail());
        view.setBilledMobile(invoice.getBilledMobile());
        view.setBilledAddress(invoice.getBilledAddress());
        view.setSubtotal(invoice.getSubtotal());
        view.setDiscount(invoice.getDiscountAmount());
        view.setTax(invoice.getTaxAmount());
        view.setTotal(invoice.getTotalAmount());
        view.setIssuedAt(invoice.getIssuedAt());
        for (OrderItem item : invoice.getOrder().getItems()) {
            OrderView.Line line = new OrderView.Line();
            line.setProductName(item.getProductNameSnapshot());
            line.setSku(item.getSkuSnapshot());
            line.setQuantity(item.getQuantity());
            line.setUnitPrice(item.getUnitPrice());
            line.setDiscount(item.getDiscountAmount());
            line.setTax(item.getTaxAmount());
            line.setLineTotal(item.getLineTotal());
            view.getLines().add(line);
        }
        return view;
    }

    private record PayResult(InvoiceView invoice, boolean declined) {
    }
}
