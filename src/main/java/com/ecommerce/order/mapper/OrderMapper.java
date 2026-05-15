package com.ecommerce.order.mapper;



import com.ecommerce.common.entity.OrderEntity;
import com.ecommerce.common.entity.OrderItemEntity;
import com.ecommerce.order.dto.OrderItemResponseDTO;
import com.ecommerce.order.dto.OrderResponseDTO;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper class for converting OrderEntity and OrderItemEntity
 * into OrderResponseDTO.
 */
@Component
public class OrderMapper {

    /**
     * Converts OrderEntity and its items into OrderResponseDTO.
     */
    public OrderResponseDTO toDTO(OrderEntity order, List<OrderItemEntity> items) {

        if (order == null) {
            return null;
        }

        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(order.getOrderId());
        response.setTotalAmount(order.getTotalAmount());
        response.setTotalQuantity(order.getTotalQuantity());
        response.setStatus(order.getStatus().getStatusName());
        response.setCreatedBy(order.getCreatedBy());
        response.setCreatedTimestamp(order.getCreatedTimestamp());
        response.setUpdatedTimestamp(order.getUpdatedTimestamp());
        response.setUpdatedBy(order.getUpdatedBy());

        response.setItems(mapOrderItemsToDTO(items));


        return response;
    }

    /**
     * Converts list of OrderItemEntity to DTO list.
     */
    private List<OrderItemResponseDTO> mapOrderItemsToDTO(List<OrderItemEntity> items) {
        return items.stream()
                .map(item -> {
                    OrderItemResponseDTO dto = new OrderItemResponseDTO();
                    dto.setOrderItemId(item.getOrderItemId());
                    dto.setOrderedQuantity(item.getOrderedQuantity());
                    return dto;
                })
                .toList();
    }
}

