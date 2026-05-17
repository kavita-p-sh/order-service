package com.ecommerce.order.repository;
import com.ecommerce.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findByUserId(UUID user);

    List<OrderEntity> findByStatus_StatusName(String statusName);

    List<OrderEntity> findByCreatedBy(String createdBy);

    List<OrderEntity> findByTotalAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);

    List<OrderEntity> findByTotalQuantityGreaterThanEqual(Integer minQuantity);


}