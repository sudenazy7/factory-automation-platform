package com.factory.automation.domain.repository;

import com.factory.automation.domain.enums.ItemStatus;
import com.factory.automation.domain.model.ProductionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionItemRepository extends JpaRepository<ProductionItem, Long> {
    Optional<ProductionItem> findBySerialNumber(String serialNumber);
    List<ProductionItem> findByWorkOrderId(Long workOrderId);
    List<ProductionItem> findByStatus(ItemStatus status);
    List<ProductionItem> findByCurrentStationId(Long stationId);
}
