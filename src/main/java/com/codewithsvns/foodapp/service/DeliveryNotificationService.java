package com.codewithsvns.foodapp.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

@Service
public class DeliveryNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public DeliveryNotificationService(
            SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public record DeliveryNotification(
            String type,
            UUID orderId,
            String message,
            Instant createdAt) {
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderAssigned(DeliveryOrderAssignedEvent event) {
        DeliveryNotification notification = new DeliveryNotification(
                "ORDER_ASSIGNED",
                event.orderId(),
                "A new order has been assigned to you.",
                Instant.now()
        );

        messagingTemplate.convertAndSendToUser(
                event.deliveryPartnerUserId().toString(),
                "/queue/notifications",
                notification
        );
    }
}