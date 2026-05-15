package com.ecommerce.order.controller;

import com.ecommerce.common.util.AppConstants;
import com.ecommerce.order.dto.OrderRequestDTO;
import com.ecommerce.order.dto.OrderResponseDTO;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * handles order related APIs
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name="Order Controller",description = "APIs for managing Orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * create a new order
     * @param request contains order details such as products, quantity.
     * @return order detail
     */
    @PostMapping
    @RolesAllowed({AppConstants.ROLE_USER, AppConstants.ROLE_ADMIN})
    @Operation(summary = "Create order",
            description = "Creates a new order with products and quantity.")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401",  description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Product not found")

    })
    public ResponseEntity<OrderResponseDTO> createOrder(
            @Valid @RequestBody OrderRequestDTO request) {

        log.info("Order creation request");

        OrderResponseDTO response = orderService.createOrder(request);

        log.info("Order created successfully");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch orders with optional filters.
     * Admin/Manager can fetch all orders.
     * User can fetch their own orders.
     *
     * @param status order status
     * @param createdBy user who created the order
     * @param minAmount minimum total amount
     * @param maxAmount maximum total amount
     * @param minQuantity minimum total quantity
     * @return list of filtered orders
     */
    @GetMapping
    @RolesAllowed({AppConstants.ROLE_USER, AppConstants.ROLE_ADMIN, AppConstants.ROLE_MANAGER})
    @Operation(summary = "Get Orders",
               description ="Fetch orders with filters like status,user, amount and quantity.Admin or Manager can view all orders, users can view their own orders only." )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders fetched successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Orders not found")
    })
    public ResponseEntity<List<OrderResponseDTO>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) Integer minQuantity) {

        log.info("Fetching orders with filters - status: {}, createdBy: {}, minAmount: {}, maxAmount: {}, minQuantity: {}",
                status, createdBy, minAmount, maxAmount, minQuantity);

        return ResponseEntity.ok(
                orderService.getOrders(status, createdBy, minAmount, maxAmount, minQuantity)
        );
    }

    /**
     * Fetch orders of currently logged-in user.
     * UserId is not exposed in URL.
     *
     * @return list of current user's orders
     */
    @GetMapping("/my")
    @RolesAllowed({AppConstants.ROLE_USER, AppConstants.ROLE_ADMIN})
    @Operation(
            summary = "Get my orders",
            description = "Fetches orders of currently logged-in user. UserId is taken from JWT/current user, not from URL."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Orders fetched successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Orders not found")
    })
    public ResponseEntity<List<OrderResponseDTO>> getCurrentUserOrders() {

        log.info("Fetching orders for current logged-in user");

        return ResponseEntity.ok(orderService.getCurrentUserOrders());
    }

    /**
     * cancel order by id
     * @param orderId id of order
     * @return updated order after cancellation
     */
    @PutMapping("/cancel/{orderId}")
    @RolesAllowed({AppConstants.ROLE_USER, AppConstants.ROLE_ADMIN, AppConstants.ROLE_MANAGER})
    @Operation(summary = "Cancel Order",
               description = "Cancel an order by ID,Users can cancel their own orders ,Admins can cancel any order on behalf of a user if needed")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order cancelled successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable UUID orderId) {
        log.warn("Cancelling order with id {}");

        OrderResponseDTO response=orderService.cancelOrder(orderId);

        log.info("Order cancelled successfully with id {}");

        return ResponseEntity.ok(response);
    }
}