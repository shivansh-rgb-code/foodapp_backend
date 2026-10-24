package com.codewithsvns.foodapp.repository;

import com.codewithsvns.foodapp.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrder_Id(UUID orderId);
}