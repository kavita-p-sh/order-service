package com.ecommerce.order.repository;

import com.ecommerce.order.entity.OrderEntity;
import com.ecommerce.order.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findByOrder(OrderEntity order);

    List<OrderItemEntity> findByOrderIn(List<OrderEntity> orders);
}