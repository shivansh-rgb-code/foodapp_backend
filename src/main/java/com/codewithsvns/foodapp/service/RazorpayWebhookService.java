
package com.codewithsvns.foodapp.service;

import com.codewithsvns.foodapp.model.CustomerOrder;
import com.codewithsvns.foodapp.model.PaymentMethod;
import com.codewithsvns.foodapp.model.PaymentStatus;
import com.codewithsvns.foodapp.repository.CustomerOrderRepository;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RazorpayWebhookService {

    private final CustomerOrderRepository customerOrderRepository;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    public RazorpayWebhookService(
            CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    @Transactional
    public void processWebhook(String rawBody, String signature) {
        if (rawBody == null || rawBody.isBlank()
                || signature == null || signature.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Webhook body and signature are required"
            );
        }

        try {
            Utils.verifyWebhookSignature(
                    rawBody,
                    signature,
                    webhookSecret
            );
        } catch (RazorpayException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Razorpay webhook signature"
            );
        }

        JSONObject event = new JSONObject(rawBody);
        String eventType = event.optString("event", "");

        // Other events are acknowledged but do not change payment status.
        if (!"payment.captured".equals(eventType)) {
            return;
        }

        JSONObject paymentEntity = event
                .getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");

        String razorpayOrderId = paymentEntity.optString("order_id", "");
        String razorpayPaymentId = paymentEntity.optString("id", "");
        String paymentStatus = paymentEntity.optString("status", "");
        String currency = paymentEntity.optString("currency", "");

        if (razorpayOrderId.isBlank()
                || razorpayPaymentId.isBlank()
                || !"captured".equals(paymentStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid captured-payment event"
            );
        }

        CustomerOrder order = customerOrderRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order for Razorpay payment was not found"
                ));

        if (order.getPaymentMethod() != PaymentMethod.RAZORPAY) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order is not configured for Razorpay"
            );
        }

        final long expectedAmount;
        final long receivedAmount;

        try {
            BigDecimal total = order.getTotalAmount();

            if (total == null) {
                throw new ArithmeticException("Missing order total");
            }

            expectedAmount = total
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();

            receivedAmount = paymentEntity.getLong("amount");
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment amount"
            );
        }

        if (!"INR".equals(currency) || receivedAmount != expectedAmount) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount or currency does not match the order"
            );
        }

        // Repeated captured events must not change an already-paid order.
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            if (razorpayPaymentId.equals(order.getRazorpayPaymentId())) {
                return;
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order is already paid with another payment"
            );
        }

        order.setRazorpayPaymentId(razorpayPaymentId);
        order.setPaymentStatus(PaymentStatus.PAID);
        customerOrderRepository.save(order);
    }
}
