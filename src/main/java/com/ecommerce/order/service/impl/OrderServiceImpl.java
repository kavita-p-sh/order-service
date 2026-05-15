package com.ecommerce.order.service.impl;

import com.ecommerce.common.entity.*;
import com.ecommerce.common.enums.OrderStatus;
import com.ecommerce.common.enums.StockReduceStatus;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.repository.*;
import com.ecommerce.common.util.AppConstants;
import com.ecommerce.common.util.CacheConstant;
import com.ecommerce.order.client.UserClient;
import com.ecommerce.order.dto.OrderItemRequestDTO;
import com.ecommerce.order.dto.OrderRequestDTO;
import com.ecommerce.order.dto.OrderResponseDTO;
import com.ecommerce.order.dto.UserResponseDTO;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final StockReduceRepository stockReduceRepository;
    private final ProductRepository productRepository;
    private final UserClient userClient;

    /**
     * Creates a new order for the currently logged-in user.
     * Validates product stock, reduces quantity,
     * saves order and order items,
     * and updates stock reduction status.
     *
     * @param request order request containing product details
     * @return created order response
     */
    @Override
    @Transactional
    @CacheEvict(value = {CacheConstant.ORDERS,CacheConstant.ORDERS_BY_USER}, allEntries = true)
    public OrderResponseDTO createOrder(OrderRequestDTO request) {

        log.info("Creating order for current logged-in user");

        UserResponseDTO user = userClient.getCurrentUser();

        if (user == null || user.getUserId() == null) {
            throw new ResourceNotFoundException(AppConstants.USER_NOT_FOUND);
        }

        Map<Long, ProductEntity> productMap = getProductMap(request.getItems());

        OrderEntity order = new OrderEntity();
        order.setUserId(user.getUserId());
        order.setStatus(getOrderStatus(OrderStatus.PENDING.name()));

        setOrderTotals(request.getItems(), productMap, order);

        OrderEntity savedOrder = orderRepository.save(order);

        List<OrderItemEntity> savedItems =
                saveOrderItems(request.getItems(), savedOrder, productMap);

        List<StockReduceEntity> stockReduceRecords =
                createStockReduceRecords(savedOrder, request.getItems());

        try {
            reduceProductStock(request.getItems(), productMap);

            updateStockReduceStatus(stockReduceRecords, StockReduceStatus.SUCCESS);

            savedOrder.setStatus(getOrderStatus(OrderStatus.PLACED.name()));

            OrderEntity placedOrder = orderRepository.save(savedOrder);

            log.info("Order placed successfully with order id: {}", placedOrder.getOrderId());

            return orderMapper.toDTO(placedOrder, savedItems);

        } catch (Exception ex) {

            log.error("Stock reduction failed for order id: {}", savedOrder.getOrderId(), ex);

            updateStockReduceStatus(stockReduceRecords, StockReduceStatus.FAILED);

            savedOrder.setStatus(getOrderStatus(OrderStatus.FAILED.name()));

            OrderEntity failedOrder = orderRepository.save(savedOrder);

            return orderMapper.toDTO(failedOrder, savedItems);

        }
    }

    /**
     * Fetches products from database
     * and maps them by productId.
     *
     * @param items order item request list
     * @return map of productId and product entity
     */
    private Map<Long, ProductEntity> getProductMap(List<OrderItemRequestDTO> items) {
        Map<Long, ProductEntity> productMap = new HashMap<>();

        for (OrderItemRequestDTO item : items) {
            ProductEntity product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(AppConstants.PRODUCT_NOT_FOUND));

            productMap.put(product.getProductId(), product);
        }

        return productMap;
    }

    /**
     * Calculates total amount and total quantity of order.
     *
     * @param items order item request list
     * @param productMap map of products
     * @param order order entity
     */
    private void setOrderTotals(List<OrderItemRequestDTO> items,
                                Map<Long, ProductEntity> productMap,
                                OrderEntity order) {

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (OrderItemRequestDTO item : items) {
            ProductEntity product = productMap.get(item.getProductId());

            if (product.getQuantity() < item.getOrderedQuantity()) {
                throw new BadRequestException(
                        AppConstants.INSUFFICIENT_PRODUCT_QUANTITY + ": " + product.getName()
                );
            }

            totalAmount = totalAmount.add(
                    product.getPrice().multiply(BigDecimal.valueOf(item.getOrderedQuantity()))
            );

            totalQuantity += item.getOrderedQuantity();
        }

        order.setTotalAmount(totalAmount);
        order.setTotalQuantity(totalQuantity);
    }

    /**
     * Saves order items for the given order.
     *
     * @param items order item request list
     * @param order saved order entity
     * @param productMap map of products
     * @return saved order items
     */
    private List<OrderItemEntity> saveOrderItems(List<OrderItemRequestDTO> items,
                                                 OrderEntity order,
                                                 Map<Long, ProductEntity> productMap) {

        return items.stream()
                .map(item -> {
                    OrderItemEntity orderItem = new OrderItemEntity();
                    orderItem.setOrder(order);
                    orderItem.setProduct(productMap.get(item.getProductId()));
                    orderItem.setOrderedQuantity(item.getOrderedQuantity());

                    return orderItemRepository.save(orderItem);
                })
                .toList();
    }

    /**
     * Reduces product stock quantity.
     *
     * @param items order item request list
     * @param productMap map of products
     */

    private void reduceProductStock(List<OrderItemRequestDTO> items,
                                    Map<Long, ProductEntity> productMap) {

        for (OrderItemRequestDTO item : items) {
            ProductEntity product = productMap.get(item.getProductId());

            if (item.getOrderedQuantity() == null || item.getOrderedQuantity() <= 0) {
                throw new BadRequestException(AppConstants.INSUFFICIENT_PRODUCT_QUANTITY);
            }

            if (product.getQuantity() < item.getOrderedQuantity()) {
                throw new BadRequestException(AppConstants.PRODUCT_QUANTITY_INVALID);
            }

            product.setQuantity(product.getQuantity() - item.getOrderedQuantity());
            productRepository.save(product);

      }
    }
    /**
     * Creates stock reduction records for tracking.
     *
     * @param order saved order
     * @param items order item request list
     * @return stock reduction records
     */

    private List<StockReduceEntity> createStockReduceRecords(OrderEntity order,
                                                             List<OrderItemRequestDTO> items) {
        return items.stream()
                .map(item -> {
                    StockReduceEntity stockReduce = new StockReduceEntity();

                    stockReduce.setOrder(order);
                    stockReduce.setProduct(productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException(AppConstants.PRODUCT_NOT_FOUND)));
                    stockReduce.setQuantity(item.getOrderedQuantity());
                    stockReduce.setStatus(StockReduceStatus.PENDING);

                    return stockReduceRepository.save(stockReduce);
                })
                .toList();
    }

    /**
     * Updates stock reduction status.
     *
     * @param stockReduceRecords stock reduction records
     * @param status updated status
     */
    private void updateStockReduceStatus(List<StockReduceEntity> stockReduceRecords,
                                         StockReduceStatus status) {
        for (StockReduceEntity stockReduce : stockReduceRecords) {
            stockReduce.setStatus(status);
            stockReduceRepository.save(stockReduce);
        }
    }

    /**
     * Fetches order status entity by status name.
     *
     * @param statusName order status name
     * @return order status entity
     */
    private OrderStatusEntity getOrderStatus(String statusName) {
        return orderStatusRepository.findByStatusName(statusName)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.STATUS_NOT_FOUND));
    }


    /**
     * Fetches order items grouped by orderId.
     *
     * @param orders order list
     * @return map of orderId and order items
     */
    private Map<UUID, List<OrderItemEntity>> getOrderItemsMap(List<OrderEntity> orders) {
        return orderItemRepository.findByOrderIn(orders).stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getOrderId()));
    }

    /**
     * Converts orders into response DTO list.
     *
     * @param orders order entity list
     * @return order response list
     */
    private List<OrderResponseDTO> mapOrdersToDTO(List<OrderEntity> orders) {
        Map<UUID, List<OrderItemEntity>> orderItemsMap = getOrderItemsMap(orders);

        return orders.stream()
                .map(order -> orderMapper.toDTO(
                        order,
                        orderItemsMap.getOrDefault(order.getOrderId(), Collections.emptyList())
                ))
                .toList();
    }

    /**
     * Fetches orders using optional filters.
     *
     * @param status order status
     * @param createdBy created by user
     * @param minAmount minimum amount
     * @param maxAmount maximum amount
     * @param minQuantity minimum quantity
     * @return filtered order list
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrders(String status,
                                            String createdBy,
                                            BigDecimal minAmount,
                                            BigDecimal maxAmount,
                                            Integer minQuantity) {

        if (status != null && !status.isBlank()) {
            return mapOrdersToDTO(orderRepository.findByStatus_StatusName(status));
        }

        if (createdBy != null && !createdBy.isBlank()) {
            return mapOrdersToDTO(orderRepository.findByCreatedBy(createdBy));
        }

        if (minAmount != null && maxAmount != null) {
            return mapOrdersToDTO(orderRepository.findByTotalAmountBetween(minAmount, maxAmount));
        }

        if (minQuantity != null) {
            return mapOrdersToDTO(orderRepository.findByTotalQuantityGreaterThanEqual(minQuantity));
        }

        return getAllOrders();
    }

    /**
     * Fetches all orders.
     *
     * @return all order responses
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstant.ORDERS, key = CacheConstant.ALL_ORDERS_KEY)
    public List<OrderResponseDTO> getAllOrders() {
        return mapOrdersToDTO(orderRepository.findAll());
    }


    /**
     * Fetches orders by userId.
     *
     * @param userId user id
     * @return user orders
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByUserId(UUID userId) {
        UserResponseDTO user = userClient.getUserById(userId);

        if (user == null || user.getUserId() == null) {
            throw new ResourceNotFoundException(AppConstants.USER_NOT_FOUND);
        }

        return mapOrdersToDTO(orderRepository.findByUserId(userId));
    }

    /**
     * Fetches orders of currently logged-in user.
     *
     * @return current user orders
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstant.ORDERS, key =" #username")
    public List<OrderResponseDTO> getCurrentUserOrders() {
        UserResponseDTO user = userClient.getCurrentUser();

        if (user == null || user.getUserId() == null) {
            throw new ResourceNotFoundException(AppConstants.USER_NOT_FOUND);
        }

        return mapOrdersToDTO(orderRepository.findByUserId(user.getUserId()));
    }

    /**
     * Cancels an existing order.
     *
     * @param orderId order id
     * @return cancelled order response
     */
    @Override
    @Transactional
    @CacheEvict(value = {CacheConstant.ORDERS, CacheConstant.ORDERS_BY_USER}, allEntries = true)
    public OrderResponseDTO cancelOrder(UUID orderId) {

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.ORDER_NOT_FOUND));

        if (OrderStatus.DELIVERED.name().equals(order.getStatus().getStatusName())) {
            throw new BadRequestException(AppConstants.CANNOT_CANCEL);
        }

        if (OrderStatus.CANCELLED.name().equals(order.getStatus().getStatusName())) {
            throw new BadRequestException(AppConstants.ORDER_ALREADY_CANCELLED);
        }

        order.setStatus(getOrderStatus(OrderStatus.CANCELLED.name()));

        return orderMapper.toDTO(
                orderRepository.save(order),
                orderItemRepository.findByOrder(order)
        );
    }
}