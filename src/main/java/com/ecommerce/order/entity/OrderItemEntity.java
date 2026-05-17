package com.ecommerce.order.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entity representing product items inside an order.
 *
 * productId is stored as a reference value only.
 * There is no direct foreign key relation with product-service database.
 */
@Entity
@Table(name = "tb_order_items")
@Data
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "ordered_quantity", nullable = false)
    private Integer orderedQuantity;
}