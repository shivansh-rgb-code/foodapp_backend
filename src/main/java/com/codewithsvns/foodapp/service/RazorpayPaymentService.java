
package com.codewithsvns.foodapp.service;

import com.codewithsvns.foodapp.model.CustomerOrder;
import com.codewithsvns.foodapp.model.PaymentMethod;
import com.codewithsvns.foodapp.model.PaymentStatus;
import com.codewithsvns.foodapp.model.OrderStatus;
import com.codewithsvns.foodapp.repository.CustomerOrderRepository;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RazorpayPaymentService {

    private static final Logger log =
            LoggerFactory.getLogger(RazorpayPaymentService.class);
    private final RazorpayClient razorpayClient;
    private final CustomerOrderRepository customerOrderRepository;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public RazorpayPaymentService(
            RazorpayClient razorpayClient,
            CustomerOrderRepository customerOrderRepository) {
        this.razorpayClient = razorpayClient;
        this.customerOrderRepository = customerOrderRepository;
    }

    @Transactional
    public String createRazorpayOrder(CustomerOrder customerOrder) {
        CustomerOrder lockedOrder = customerOrderRepository
                .findByIdForUpdate(customerOrder.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));

        if (lockedOrder.getPaymentMethod() != PaymentMethod.RAZORPAY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order is not configured for Razorpay payment"
            );
        }

        if (lockedOrder.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment cannot be started for a cancelled order"
            );
        }

        if (lockedOrder.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order has already been paid"
            );
        }

        if (lockedOrder.getRazorpayOrderId() != null) {
            return lockedOrder.getRazorpayOrderId();
        }

        BigDecimal totalAmount = lockedOrder.getTotalAmount();

        if (totalAmount == null
                || totalAmount.compareTo(BigDecimal.ONE) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount must be at least ₹1.00"
            );
        }

        final long amountInPaise;

        try {
            amountInPaise = totalAmount
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order amount"
            );
        }

        try {
            JSONObject request = new JSONObject();
            request.put("amount", amountInPaise);
            request.put("currency", "INR");
            request.put("receipt", lockedOrder.getId().toString());

            JSONObject notes = new JSONObject();
            notes.put("customer_order_id", lockedOrder.getId().toString());
            request.put("notes", notes);

            Order razorpayOrder = razorpayClient.orders.create(request);
            String razorpayOrderId = razorpayOrder.get("id");

            lockedOrder.setRazorpayOrderId(razorpayOrderId);
            customerOrderRepository.save(lockedOrder);

            return razorpayOrderId;

        } catch (RazorpayException exception) {
            log.error(
                    "Razorpay order creation failed. status={}, code={}, description={}",
                    exception.getStatusCode(),
                    exception.getCode(),
                    exception.getDescription(),
                    exception
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to create Razorpay payment order"
            );
        }
    }

    @Transactional
    public PaymentStatus verifyPayment(
            UUID orderId,
            UUID customerId,
            String submittedOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {

        CustomerOrder customerOrder = customerOrderRepository
                .findByIdAndCustomerIdForUpdate(orderId, customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"));

        if (customerOrder.getPaymentMethod() != PaymentMethod.RAZORPAY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This order does not use Razorpay");
        }

        if (customerOrder.getPaymentStatus() == PaymentStatus.PAID) {
            if (razorpayPaymentId != null
                    && razorpayPaymentId.equals(customerOrder.getRazorpayPaymentId())) {
                return PaymentStatus.PAID;
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order has already been paid with another payment");
        }

        String savedOrderId = customerOrder.getRazorpayOrderId();

        if (savedOrderId == null || !savedOrderId.equals(submittedOrderId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Razorpay order ID does not match this order");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", savedOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);

            boolean validSignature =
                    Utils.verifyPaymentSignature(options, razorpayKeySecret);

            if (!validSignature) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Razorpay payment signature is invalid");
            }

            Payment payment = razorpayClient.payments.fetch(razorpayPaymentId);

            String actualOrderId = payment.get("order_id");
            String actualStatus = payment.get("status");
            String actualCurrency = payment.get("currency");

            long expectedAmount = customerOrder.getTotalAmount()
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();

            long actualAmount =
                    Long.parseLong(String.valueOf(payment.get("amount")));

            if (!savedOrderId.equals(actualOrderId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Payment belongs to a different Razorpay order");
            }

            if (!"captured".equals(actualStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Payment is not captured yet");
            }

            if (!"INR".equals(actualCurrency)
                    || actualAmount != expectedAmount) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Payment currency or amount does not match the order");
            }

            customerOrder.setRazorpayPaymentId(razorpayPaymentId);
            customerOrder.setPaymentStatus(PaymentStatus.PAID);

            customerOrderRepository.save(customerOrder);

            return PaymentStatus.PAID;

        } catch (RazorpayException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to verify payment with Razorpay");
        } catch (ArithmeticException | NumberFormatException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment amount");
        }
    }

}
