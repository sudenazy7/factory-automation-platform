package com.factory.automation.domain.repository;

import com.factory.automation.domain.enums.WorkOrderStatus;
import com.factory.automation.domain.model.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    Optional<WorkOrder> findByOrderNumber(String orderNumber);
    List<WorkOrder> findByStatus(WorkOrderStatus status);
    List<WorkOrder> findByStatusNot(WorkOrderStatus status);
}
