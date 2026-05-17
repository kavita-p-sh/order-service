package com.ecommerce.order.repository;


import com.ecommerce.order.entity.OrderEntity;
import com.ecommerce.order.entity.StockReduceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockReduceRepository extends JpaRepository<StockReduceEntity, Long> {

    List<StockReduceEntity> findByOrder(OrderEntity order);
}