package com.codewithsvns.foodapp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    @Column(name = "food_name", nullable = false, length = 150)
    private String foodName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    protected OrderItem() {
    }

    public OrderItem(
            FoodItem foodItem,
            String foodName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal) {
        this.foodItem = foodItem;
        this.foodName = foodName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public void setOrder(CustomerOrder order) {
        this.order = order;
    }

    public UUID getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public FoodItem getFoodItem() {
        return foodItem;
    }

    public String getFoodName() {
        return foodName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}