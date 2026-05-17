package com.ecommerce.order.entity;

import com.ecommerce.common.enums.StockReduceStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity used to track stock reduction status for order items.
 *
 * This helps in maintaining order processing state such as
 * PENDING, SUCCESS or FAILED.
 */
@Entity
@Table(name = "tb_stock_reduce")
@Data
@EqualsAndHashCode(callSuper = true)
public class StockReduceEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long stockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StockReduceStatus status;
}