
package com.codewithsvns.foodapp.repository;

import com.codewithsvns.foodapp.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import com.codewithsvns.foodapp.model.OrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, UUID> {

    List<CustomerOrder> findByCustomer_IdOrderByCreatedAtDesc(
            UUID customerId);

    Optional<CustomerOrder> findByIdAndCustomer_Id(
            UUID orderId, UUID customerId);

    List<CustomerOrder> findByRestaurant_IdOrderByCreatedAtDesc(
            UUID restaurantId);

    Optional<CustomerOrder> findByIdAndRestaurant_Id(
            UUID orderId, UUID restaurantId);

    List<CustomerOrder> findByDeliveryPartner_IdOrderByCreatedAtDesc(
            UUID deliveryPartnerId);

    Optional<CustomerOrder> findByIdAndDeliveryPartner_Id(
            UUID orderId, UUID deliveryPartnerId);

    boolean existsByDeliveryPartner_IdAndStatusIn(
            UUID deliveryPartnerId,
            Collection<OrderStatus> statuses);

    Optional<CustomerOrder> findByRazorpayOrderId(String razorpayOrderId);
}
