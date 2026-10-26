
package com.codewithsvns.foodapp.repository;

import com.codewithsvns.foodapp.model.CustomerOrder;
import com.codewithsvns.foodapp.model.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    List<CustomerOrder> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    Optional<CustomerOrder> findByIdAndCustomer_Id(UUID orderId, UUID customerId);

    List<CustomerOrder> findByRestaurant_IdOrderByCreatedAtDesc(UUID restaurantId);

    Optional<CustomerOrder> findByIdAndRestaurant_Id(UUID orderId, UUID restaurantId);

    List<CustomerOrder> findByDeliveryPartner_IdOrderByCreatedAtDesc(UUID deliveryPartnerId);

    Optional<CustomerOrder> findByIdAndDeliveryPartner_Id(UUID orderId, UUID deliveryPartnerId);

    boolean existsByDeliveryPartner_IdAndStatusIn(UUID deliveryPartnerId, Collection<OrderStatus> statuses);

    Optional<CustomerOrder> findByRazorpayOrderId(String razorpayOrderId);

    List<CustomerOrder> findByStatusAndDeliveryPartnerIsNullOrderByCreatedAtAsc(OrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :orderId")
    Optional<CustomerOrder> findByIdForUpdate(@Param("orderId") UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.razorpayOrderId = :razorpayOrderId")
    Optional<CustomerOrder> findByRazorpayOrderIdForUpdate(
            @Param("razorpayOrderId") String razorpayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :orderId and o.customer.id = :customerId")
    Optional<CustomerOrder> findByIdAndCustomerIdForUpdate(
            @Param("orderId") UUID orderId,
            @Param("customerId") UUID customerId);
}
