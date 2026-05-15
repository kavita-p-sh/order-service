package com.ecommerce.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderItemResponseDTO implements Serializable {
    private Long orderItemId;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer orderedQuantity;
}