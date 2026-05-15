package com.ecommerce.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class OrderItemRequestDTO {


    @NotNull(message = "{orderItem.productId.required}")
    private Long productId;

    @NotNull(message = "{orderItem.quantity.required}")
    @Positive(message = "{orderItem.quantity.positive}")
    private Integer orderedQuantity;
}
