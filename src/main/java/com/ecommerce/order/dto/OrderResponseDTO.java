package com.ecommerce.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OrderResponseDTO implements Serializable {

    private UUID orderId;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private String status;
    private LocalDateTime createdTimestamp;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime updatedTimestamp;

    private List<OrderItemResponseDTO> items;

}