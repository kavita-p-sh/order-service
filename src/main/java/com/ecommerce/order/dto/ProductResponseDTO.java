package com.ecommerce.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductResponseDTO {

    private Long productId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
}