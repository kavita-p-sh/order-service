package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderRequestDTO;
import com.ecommerce.order.dto.OrderResponseDTO;
import com.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    void createOrder_success() {
        OrderRequestDTO request = new OrderRequestDTO();

        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(UUID.randomUUID());
        response.setStatus("PLACED");
        response.setTotalAmount(BigDecimal.valueOf(124000));
        response.setTotalQuantity(2);

        when(orderService.createOrder(request)).thenReturn(response);

        ResponseEntity<OrderResponseDTO> result =
                orderController.createOrder(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("PLACED", result.getBody().getStatus());
        assertEquals(BigDecimal.valueOf(124000), result.getBody().getTotalAmount());
        assertEquals(2, result.getBody().getTotalQuantity());

        verify(orderService).createOrder(request);
    }
    @Test
    void createOrder_shouldThrowException_WhenServiceThrowsException() {

        OrderRequestDTO request = new OrderRequestDTO();

        when(orderService.createOrder(request))
                .thenThrow(new RuntimeException("Order creation failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderController.createOrder(request)
        );

        assertEquals("Order creation failed", exception.getMessage());

        verify(orderService).createOrder(request);
    }


    @Test
    void getOrders_success() {
        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(UUID.randomUUID());
        response.setStatus("PLACED");
        response.setTotalAmount(BigDecimal.valueOf(50000));
        response.setTotalQuantity(3);

        when(orderService.getOrders("PLACED", "Sumitsingh",
                BigDecimal.valueOf(1000), BigDecimal.valueOf(100000), 1))
                .thenReturn(List.of(response));

        ResponseEntity<List<OrderResponseDTO>> result =
                orderController.getOrders("PLACED", "Sumitsingh",
                        BigDecimal.valueOf(1000), BigDecimal.valueOf(100000), 1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        assertEquals("PLACED", result.getBody().get(0).getStatus());

        verify(orderService).getOrders("PLACED", "Sumitsingh",
                BigDecimal.valueOf(1000), BigDecimal.valueOf(100000), 1);
    }
    @Test
    void getOrders_shouldThrowException_WhenServiceThrowsException() {

        when(orderService.getOrders(
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenThrow(new RuntimeException("Orders not found"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderController.getOrders(
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        assertEquals("Orders not found", exception.getMessage());

        verify(orderService).getOrders(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }


    @Test
    void getCurrentUserOrders_success() {
        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(UUID.randomUUID());
        response.setStatus("PLACED");
        response.setTotalAmount(BigDecimal.valueOf(20000));
        response.setTotalQuantity(1);

        when(orderService.getCurrentUserOrders()).thenReturn(List.of(response));

        ResponseEntity<List<OrderResponseDTO>> result =
                orderController.getCurrentUserOrders();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        assertEquals("PLACED", result.getBody().get(0).getStatus());

        verify(orderService).getCurrentUserOrders();
    }

    @Test
    void getCurrentUserOrders_shouldThrowException_WhenServiceThrowsException() {

        when(orderService.getCurrentUserOrders())
                .thenThrow(new RuntimeException("User orders not found"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderController.getCurrentUserOrders()
        );

        assertEquals("User orders not found", exception.getMessage());

        verify(orderService).getCurrentUserOrders();
    }
    @Test
    void cancelOrder_success() {
        UUID orderId = UUID.randomUUID();

        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(orderId);
        response.setStatus("CANCELLED");
        response.setTotalAmount(BigDecimal.valueOf(124000));
        response.setTotalQuantity(2);

        when(orderService.cancelOrder(orderId)).thenReturn(response);

        ResponseEntity<OrderResponseDTO> result =
                orderController.cancelOrder(orderId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(orderId, result.getBody().getOrderId());
        assertEquals("CANCELLED", result.getBody().getStatus());

        verify(orderService).cancelOrder(orderId);
    }

    @Test
    void cancelOrder_shouldThrowException_WhenOrderNotFound() {

        UUID orderId = UUID.randomUUID();

        when(orderService.cancelOrder(orderId))
                .thenThrow(new RuntimeException("Order not found"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderController.cancelOrder(orderId)
        );

        assertEquals("Order not found", exception.getMessage());

        verify(orderService).cancelOrder(orderId);
    }
}
