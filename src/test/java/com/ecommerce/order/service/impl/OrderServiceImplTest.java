package com.ecommerce.order.service.impl;

import com.ecommerce.common.enums.OrderStatus;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.util.AppConstants;
import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.client.UserClient;
import com.ecommerce.order.dto.OrderItemRequestDTO;
import com.ecommerce.order.dto.OrderRequestDTO;
import com.ecommerce.order.dto.OrderResponseDTO;
import com.ecommerce.order.dto.ProductResponseDTO;
import com.ecommerce.order.dto.UserResponseDTO;
import com.ecommerce.order.entity.OrderEntity;
import com.ecommerce.order.entity.OrderItemEntity;
import com.ecommerce.order.entity.OrderStatusEntity;
import com.ecommerce.order.entity.StockReduceEntity;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.repository.OrderItemRepository;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OrderStatusRepository;
import com.ecommerce.order.repository.StockReduceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private StockReduceRepository stockReduceRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private StockReduceAuditService stockReduceAuditService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID userId;
    private UUID orderId;

    private UserResponseDTO userResponseDTO;
    private ProductResponseDTO product;
    private OrderStatusEntity pendingStatus;
    private OrderStatusEntity placedStatus;
    private OrderStatusEntity failedStatus;
    private OrderStatusEntity cancelledStatus;
    private OrderResponseDTO orderResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        userResponseDTO = new UserResponseDTO();
        userResponseDTO.setUserId(userId);

        product = new ProductResponseDTO();
        product.setProductId(1L);
        product.setName("Laptop");
        product.setPrice(BigDecimal.valueOf(62000));
        product.setQuantity(10);

        pendingStatus = createStatus(OrderStatus.PENDING.name());
        placedStatus = createStatus(OrderStatus.PLACED.name());
        failedStatus = createStatus(OrderStatus.FAILED.name());
        cancelledStatus = createStatus(OrderStatus.CANCELLED.name());

        orderResponseDTO = mock(OrderResponseDTO.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrder_ShouldCreateOrder_WhenRequestIsValid() {
        OrderRequestDTO request = createOrderRequest();

        when(userClient.getCurrentUser()).thenReturn(userResponseDTO);
        when(productClient.getProductById(1L)).thenReturn(product);

        when(orderStatusRepository.findByStatusName(OrderStatus.PENDING.name()))
                .thenReturn(Optional.of(pendingStatus));

        when(orderStatusRepository.findByStatusName(OrderStatus.PLACED.name()))
                .thenReturn(Optional.of(placedStatus));

        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> {
                    OrderEntity order = invocation.getArgument(0);
                    order.setOrderId(orderId);
                    return order;
                });

        when(orderItemRepository.save(any(OrderItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(stockReduceRepository.save(any(StockReduceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderMapper.toDTO(any(OrderEntity.class), anyList()))
                .thenReturn(orderResponseDTO);

        OrderResponseDTO result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(orderResponseDTO, result);

        verify(userClient).getCurrentUser();
        verify(productClient).getProductById(1L);
        verify(productClient).reduceStock(1L, 2);
        verify(orderRepository, times(2)).save(any(OrderEntity.class));
        verify(orderItemRepository).save(any(OrderItemEntity.class));
        verify(stockReduceRepository, times(2)).save(any(StockReduceEntity.class));
        verify(orderMapper).toDTO(any(OrderEntity.class), anyList());
        verifyNoInteractions(stockReduceAuditService);
    }

    @Test
    void createOrder_ShouldThrowResourceNotFoundException_WhenCurrentUserIsNull() {
        OrderRequestDTO request = createOrderRequest();

        when(userClient.getCurrentUser()).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(AppConstants.USER_NOT_FOUND, exception.getMessage());

        verify(userClient).getCurrentUser();
        verifyNoInteractions(productClient);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_ShouldThrowResourceNotFoundException_WhenUserIdIsNull() {
        OrderRequestDTO request = createOrderRequest();

        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(null);

        when(userClient.getCurrentUser()).thenReturn(user);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(AppConstants.USER_NOT_FOUND, exception.getMessage());

        verify(userClient).getCurrentUser();
        verifyNoInteractions(productClient);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_ShouldThrowResourceNotFoundException_WhenProductNotFound() {
        OrderRequestDTO request = createOrderRequest();

        when(userClient.getCurrentUser()).thenReturn(userResponseDTO);
        when(productClient.getProductById(1L)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(AppConstants.PRODUCT_NOT_FOUND, exception.getMessage());

        verify(userClient).getCurrentUser();
        verify(productClient).getProductById(1L);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_ShouldThrowBadRequestException_WhenProductQuantityIsInsufficient() {
        OrderRequestDTO request = createOrderRequest();

        product.setQuantity(1);

        when(userClient.getCurrentUser()).thenReturn(userResponseDTO);
        when(productClient.getProductById(1L)).thenReturn(product);

        when(orderStatusRepository.findByStatusName(OrderStatus.PENDING.name()))
                .thenReturn(Optional.of(pendingStatus));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.createOrder(request)
        );

        assertTrue(exception.getMessage()
                .contains(AppConstants.INSUFFICIENT_PRODUCT_QUANTITY));

        verify(userClient).getCurrentUser();
        verify(productClient).getProductById(1L);
        verify(orderStatusRepository).findByStatusName(OrderStatus.PENDING.name());
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_ShouldReturnFailedOrder_WhenStockReductionFails() {
        OrderRequestDTO request = createOrderRequest();

        OrderEntity failedOrder = createOrderEntity(failedStatus);

        when(userClient.getCurrentUser()).thenReturn(userResponseDTO);
        when(productClient.getProductById(1L)).thenReturn(product);

        when(orderStatusRepository.findByStatusName(OrderStatus.PENDING.name()))
                .thenReturn(Optional.of(pendingStatus));

        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> {
                    OrderEntity order = invocation.getArgument(0);
                    order.setOrderId(orderId);
                    return order;
                });

        when(orderItemRepository.save(any(OrderItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(stockReduceRepository.save(any(StockReduceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("Product service error"))
                .when(productClient).reduceStock(1L, 2);

        when(stockReduceAuditService.OrderAsFailed(any(OrderEntity.class), anyList()))
                .thenReturn(failedOrder);

        when(orderMapper.toDTO(any(OrderEntity.class), anyList()))
                .thenReturn(orderResponseDTO);

        OrderResponseDTO result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(orderResponseDTO, result);

        verify(productClient).reduceStock(1L, 2);
        verify(stockReduceAuditService).OrderAsFailed(any(OrderEntity.class), anyList());
        verify(orderMapper).toDTO(eq(failedOrder), anyList());
    }

    @Test
    void getAllOrders_ShouldReturnOrderResponseList() {
        OrderEntity order = createOrderEntity(placedStatus);
        OrderItemEntity item = createOrderItem(order);

        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(orderItemRepository.findByOrderIn(List.of(order))).thenReturn(List.of(item));
        when(orderMapper.toDTO(eq(order), anyList())).thenReturn(orderResponseDTO);

        List<OrderResponseDTO> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(orderResponseDTO, result.get(0));

        verify(orderRepository).findAll();
        verify(orderItemRepository).findByOrderIn(List.of(order));
        verify(orderMapper).toDTO(eq(order), anyList());
    }

    @Test
    void getOrders_ShouldReturnOrdersByStatus_WhenStatusFilterGiven() {
        OrderEntity order = createOrderEntity(placedStatus);
        OrderItemEntity item = createOrderItem(order);

        when(orderRepository.findByStatus_StatusName(OrderStatus.PLACED.name()))
                .thenReturn(List.of(order));

        when(orderItemRepository.findByOrderIn(List.of(order))).thenReturn(List.of(item));
        when(orderMapper.toDTO(eq(order), anyList())).thenReturn(orderResponseDTO);

        List<OrderResponseDTO> result = orderService.getOrders(
                OrderStatus.PLACED.name(),
                null,
                null,
                null,
                null
        );

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(orderRepository).findByStatus_StatusName(OrderStatus.PLACED.name());
        verify(orderItemRepository).findByOrderIn(List.of(order));
    }

    @Test
    void getOrders_ShouldReturnOrdersByCreatedBy_WhenCreatedByFilterGiven() {
        OrderEntity order = createOrderEntity(placedStatus);
        OrderItemEntity item = createOrderItem(order);

        when(orderRepository.findByCreatedBy("kavitaprajapati"))
                .thenReturn(List.of(order));

        when(orderItemRepository.findByOrderIn(List.of(order))).thenReturn(List.of(item));
        when(orderMapper.toDTO(eq(order), anyList())).thenReturn(orderResponseDTO);

        List<OrderResponseDTO> result = orderService.getOrders(
                null,
                "kavitaprajapati",
                null,
                null,
                null
        );

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(orderRepository).findByCreatedBy("kavitaprajapati");
    }

    @Test
    void getOrders_ShouldReturnOrdersByAmountRange_WhenMinAndMaxAmountGiven() {
        OrderEntity order = createOrderEntity(placedStatus);
        OrderItemEntity item = createOrderItem(order);

        BigDecimal minAmount = BigDecimal.valueOf(1000);
        BigDecimal maxAmount = BigDecimal.valueOf(70000);

        when(orderRepository.findByTotalAmountBetween(minAmount, maxAmount))
                .thenReturn(List.of(order));

        when(orderItemRepository.findByOrderIn(List.of(order))).thenReturn(List.of(item));
        when(orderMapper.toDTO(eq(order), anyList())).thenReturn(orderResponseDTO);

        List<OrderResponseDTO> result = orderService.getOrders(
                null,
                null,
                minAmount,
                maxAmount,
                null
        );

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(orderRepository).findByTotalAmountBetween(minAmount, maxAmount);
    }

    @Test
    void getOrders_ShouldReturnOrdersByMinQuantity_WhenMinQuantityGiven() {
        OrderEntity order = createOrderEntity(placedStatus);
        OrderItemEntity item = createOrderItem(order);

        when(orderRepository.findByTotalQuantityGreaterThanEqual(2))
                .thenReturn(List.of(order));

        when(orderItemRepository.findByOrderIn(List.of(order))).thenReturn(List.of(item));
        when(orderMapper.toDTO(eq(order), anyList())).thenReturn(orderResponseDTO);

        List<OrderResponseDTO> result = orderService.getOrders(
                null,
                null,
                null,
                null,
                2
        );

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(orderRepository).findByTotalQuantityGreaterThanEqual(2);
    }

    @Test
    void getOrdersByUserId_ShouldReturnUserOrders_WhenUserExists() {
        OrderEntity order = createOrderEntity(placedStatus);
        OrderItemEntity item = createOrderItem(order);

        when(userClient.getUserById(userId)).thenReturn(userResponseDTO);
        when(orderRepository.findByUserId(userId)).thenReturn(List.of(order));
        when(orderItemRepository.findByOrderIn(List.of(order))).thenReturn(List.of(item));
        when(orderMapper.toDTO(eq(order), anyList())).thenReturn(orderResponseDTO);

        List<OrderResponseDTO> result = orderService.getOrdersByUserId(userId);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(userClient).getUserById(userId);
        verify(orderRepository).findByUserId(userId);
    }

    @Test
    void getOrdersByUserId_ShouldThrowResourceNotFoundException_WhenUserNotFound() {
        when(userClient.getUserById(userId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getOrdersByUserId(userId)
        );

        assertEquals(AppConstants.USER_NOT_FOUND, exception.getMessage());

        verify(userClient).getUserById(userId);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void getCurrentUserOrders_ShouldReturnCurrentUserOrders_WhenUserExists() {
        OrderEntity order = createOrderEntity(placedStatus);
        OrderItemEntity item = createOrderItem(order);

        when(userClient.getCurrentUser()).thenReturn(userResponseDTO);
        when(orderRepository.findByUserId(userId)).thenReturn(List.of(order));
        when(orderItemRepository.findByOrderIn(List.of(order))).thenReturn(List.of(item));
        when(orderMapper.toDTO(eq(order), anyList())).thenReturn(orderResponseDTO);

        List<OrderResponseDTO> result = orderService.getCurrentUserOrders();

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(userClient).getCurrentUser();
        verify(orderRepository).findByUserId(userId);
    }

    @Test
    void getCurrentUserOrders_ShouldThrowResourceNotFoundException_WhenCurrentUserIsNull() {
        when(userClient.getCurrentUser()).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getCurrentUserOrders()
        );

        assertEquals(AppConstants.USER_NOT_FOUND, exception.getMessage());

        verify(userClient).getCurrentUser();
        verifyNoInteractions(orderRepository);
    }

    @Test
    void cancelOrder_ShouldCancelOrder_WhenOrderIsValidAndUserIsOwner() {
        setAuthentication("kavitaprajapati", "ROLE_USER");

        OrderEntity order = createOrderEntity(placedStatus);
        OrderItemEntity item = createOrderItem(order);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        when(orderStatusRepository.findByStatusName(OrderStatus.CANCELLED.name()))
                .thenReturn(Optional.of(cancelledStatus));

        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderItemRepository.findByOrder(order)).thenReturn(List.of(item));

        when(orderMapper.toDTO(any(OrderEntity.class), anyList()))
                .thenReturn(orderResponseDTO);

        OrderResponseDTO result = orderService.cancelOrder(orderId);

        assertNotNull(result);
        assertEquals(orderResponseDTO, result);
        assertEquals(OrderStatus.CANCELLED.name(), order.getStatus().getStatusName());

        verify(orderRepository).findById(orderId);
        verify(productClient).restoreStock(1L, 2);
        verify(orderStatusRepository).findByStatusName(OrderStatus.CANCELLED.name());
        verify(orderRepository).save(order);
        verify(orderItemRepository).findByOrder(order);
        verify(orderMapper).toDTO(eq(order), anyList());
    }

    @Test
    void cancelOrder_ShouldCancelOrder_WhenUserIsAdmin() {
        setAuthentication("admin", "ROLE_ADMIN");

        OrderEntity order = createOrderEntity(placedStatus);
        order.setCreatedBy("anotherUser");

        OrderItemEntity item = createOrderItem(order);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        when(orderStatusRepository.findByStatusName(OrderStatus.CANCELLED.name()))
                .thenReturn(Optional.of(cancelledStatus));

        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderItemRepository.findByOrder(order)).thenReturn(List.of(item));

        when(orderMapper.toDTO(any(OrderEntity.class), anyList()))
                .thenReturn(orderResponseDTO);

        OrderResponseDTO result = orderService.cancelOrder(orderId);

        assertNotNull(result);

        verify(productClient).restoreStock(1L, 2);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_ShouldThrowResourceNotFoundException_WhenOrderNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.cancelOrder(orderId)
        );

        assertEquals(AppConstants.ORDER_NOT_FOUND, exception.getMessage());

        verify(orderRepository).findById(orderId);
        verifyNoInteractions(orderStatusRepository);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void cancelOrder_ShouldThrowBadRequestException_WhenCurrentUserIsNotOwner() {
        setAuthentication("otherUser", "ROLE_USER");

        OrderEntity order = createOrderEntity(placedStatus);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.cancelOrder(orderId)
        );

        assertEquals(AppConstants.NOT_ALLOWED_TO_CANCEL_ORDER, exception.getMessage());

        verify(orderRepository).findById(orderId);
        verifyNoInteractions(orderStatusRepository);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void cancelOrder_ShouldThrowBadRequestException_WhenOrderAlreadyDelivered() {
        setAuthentication("kavitaprajapati", "ROLE_USER");

        OrderStatusEntity deliveredStatus = createStatus(OrderStatus.DELIVERED.name());
        OrderEntity order = createOrderEntity(deliveredStatus);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.cancelOrder(orderId)
        );

        assertEquals(AppConstants.CANNOT_CANCEL, exception.getMessage());

        verify(orderRepository).findById(orderId);
        verifyNoInteractions(orderStatusRepository);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void cancelOrder_ShouldThrowBadRequestException_WhenOrderAlreadyCancelled() {
        setAuthentication("kavitaprajapati", "ROLE_USER");

        OrderEntity order = createOrderEntity(cancelledStatus);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.cancelOrder(orderId)
        );

        assertEquals(AppConstants.ORDER_ALREADY_CANCELLED, exception.getMessage());

        verify(orderRepository).findById(orderId);
        verifyNoInteractions(orderStatusRepository);
        verifyNoInteractions(orderMapper);
    }

    private OrderRequestDTO createOrderRequest() {
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setProductId(1L);
        item.setOrderedQuantity(2);

        OrderRequestDTO request = new OrderRequestDTO();
        request.setItems(List.of(item));

        return request;
    }

    private OrderStatusEntity createStatus(String statusName) {
        OrderStatusEntity status = new OrderStatusEntity();
        status.setStatusName(statusName);
        return status;
    }

    private OrderEntity createOrderEntity(OrderStatusEntity status) {
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setStatus(status);
        order.setTotalAmount(BigDecimal.valueOf(124000));
        order.setTotalQuantity(2);
        order.setCreatedBy("kavitaprajapati");
        return order;
    }

    private OrderItemEntity createOrderItem(OrderEntity order) {
        OrderItemEntity item = new OrderItemEntity();
        item.setOrder(order);
        item.setProductId(1L);
        item.setOrderedQuantity(2);
        return item;
    }

    private void setAuthentication(String username, String role) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}