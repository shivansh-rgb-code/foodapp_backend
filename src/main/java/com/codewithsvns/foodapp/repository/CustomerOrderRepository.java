package com.codewithsvns.foodapp.repository;

import com.codewithsvns.foodapp.model.CustomerOrder;
import com.codewithsvns.foodapp.model.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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



    interface OrderEstimateInfo {
        Double getRestaurantLatitude();
        Double getRestaurantLongitude();
        Double getDeliveryLatitude();
        Double getDeliveryLongitude();
        Instant getCreatedAt();
        OrderStatus getStatus();
    }

    @Query("select o.restaurant.latitude as restaurantLatitude, "
            + "o.restaurant.longitude as restaurantLongitude, "
            + "o.deliveryLatitude as deliveryLatitude, "
            + "o.deliveryLongitude as deliveryLongitude, "
            + "o.createdAt as createdAt, o.status as status "
            + "from CustomerOrder o "
            + "where o.id = :orderId and o.customer.id = :customerId")
    Optional<OrderEstimateInfo> findEstimateInfo(
            @Param("orderId") UUID orderId,
            @Param("customerId") UUID customerId);


    interface OrderTrackingInfo {
        OrderStatus getStatus();
        UUID getDeliveryPartnerId();
        Double getDeliveryPartnerLatitude();
        Double getDeliveryPartnerLongitude();
        Instant getLocationUpdatedAt();
    }

    @Query("select o.status as status, "
            + "dp.id as deliveryPartnerId, "
            + "dp.latitude as deliveryPartnerLatitude, "
            + "dp.longitude as deliveryPartnerLongitude, "
            + "dp.locationUpdatedAt as locationUpdatedAt "
            + "from CustomerOrder o "
            + "left join o.deliveryPartner dp "
            + "where o.id = :orderId and o.customer.id = :customerId")
    Optional<OrderTrackingInfo> findTrackingInfo(
            @Param("orderId") UUID orderId,
            @Param("customerId") UUID customerId);

    @Query("select o.restaurant.id from CustomerOrder o "
            + "where o.customer.id = :customerId "
            + "and o.status = com.codewithsvns.foodapp.model.OrderStatus.DELIVERED "
            + "group by o.restaurant.id "
            + "order by count(o) desc, max(o.createdAt) desc")
    List<UUID> findFavouriteRestaurantIds(
            @Param("customerId") UUID customerId,
            Pageable pageable);

    @Query("select o.restaurant.cuisine from CustomerOrder o "
            + "where o.customer.id = :customerId "
            + "and o.status = com.codewithsvns.foodapp.model.OrderStatus.DELIVERED "
            + "and o.restaurant.cuisine is not null "
            + "group by o.restaurant.cuisine "
            + "order by count(o) desc, max(o.createdAt) desc")
    List<String> findTopCuisines(
            @Param("customerId") UUID customerId,
            Pageable pageable);
}
