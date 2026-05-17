package com.ecommerce.order.service.impl;

import com.ecommerce.common.enums.OrderStatus;
import com.ecommerce.common.enums.StockReduceStatus;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.util.AppConstants;
import com.ecommerce.order.entity.OrderEntity;
import com.ecommerce.order.entity.OrderStatusEntity;
import com.ecommerce.order.entity.StockReduceEntity;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OrderStatusRepository;
import com.ecommerce.order.repository.StockReduceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for saving stock reduction audit updates
 * in a separate transaction.
 */
@Service
@RequiredArgsConstructor
public class StockReduceAuditService {

    private final StockReduceRepository stockReduceRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;

    /**
     * Marks stock reduction records as FAILED and updates order status to FAILED.
     * This method uses a separate transaction so failure audit information
     * can be persisted independently.
     *
     * @param order order that failed during stock reduction
     * @param stockReduceRecords stock reduction records related to the order
     * @return updated failed order
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderEntity OrderAsFailed(OrderEntity order,
                                         List<StockReduceEntity> stockReduceRecords) {

        for (StockReduceEntity stockReduce : stockReduceRecords) {
            stockReduce.setStatus(StockReduceStatus.FAILED);
            stockReduceRepository.save(stockReduce);
        }

        order.setStatus(getOrderStatus(OrderStatus.FAILED.name()));
        return orderRepository.save(order);
    }

    private OrderStatusEntity getOrderStatus(String statusName) {
        return orderStatusRepository.findByStatusName(statusName)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.STATUS_NOT_FOUND));
    }
}