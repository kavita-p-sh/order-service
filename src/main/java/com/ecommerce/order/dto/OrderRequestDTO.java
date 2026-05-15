package com.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequestDTO {


    @NotEmpty(message = "{order.items.required}")
    @Valid
    private List<OrderItemRequestDTO> items;
}