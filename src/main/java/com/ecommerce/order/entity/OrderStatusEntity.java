package com.ecommerce.order.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entity representing order status values such as
 * PLACED, CANCELLED, DELIVERED, PENDING and FAILED.
 */
@Entity
@Table(name = "tb_order_status")
@Data
public class OrderStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long statusId;

    @Column(name = "status_name", nullable = false, unique = true, length = 50)
    private String statusName;
}