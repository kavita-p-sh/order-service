package com.ecommerce.order.service;

import com.ecommerce.order.dto.OrderRequestDTO;
import com.ecommerce.order.dto.OrderResponseDTO;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing order operations.
 */
public interface OrderService {

    /**
     * Creates a new order.
     *
     * @param request order request data
     * @return created order details
     */
    OrderResponseDTO createOrder(OrderRequestDTO request);

    /**
     * Fetch orders based on optional filters.
     *
     * @param status order status
     * @param createdBy user who created the order
     * @param minAmount minimum total amount
     * @param maxAmount maximum total amount
     * @param minQuantity minimum total quantity
     * @return list of filtered orders
     */
    List<OrderResponseDTO> getOrders(String status,
                                     String createdBy,
                                     BigDecimal minAmount,
                                     BigDecimal maxAmount,
                                     Integer minQuantity);

    /**
     * Fetch all orders.
     *
     * @return list of orders
     */
    List<OrderResponseDTO> getAllOrders();

    /**
     * Fetch orders by userId.
     *
     * @param userId user id
     * @return list of orders belonging to the given user
     */
    List<OrderResponseDTO> getOrdersByUserId(UUID userId);

    @Transactional(readOnly = true)
    List<OrderResponseDTO>
    getCurrentUserOrders();

    /**
     * Cancels an order by id.
     *
     * @param orderId order id
     * @return cancelled order details
     */
    OrderResponseDTO cancelOrder(UUID orderId);
}