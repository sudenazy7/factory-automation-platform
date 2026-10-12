package com.factory.automation.domain.repository;

import com.factory.automation.domain.model.ProductionItemHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionItemHistoryRepository extends JpaRepository<ProductionItemHistory, Long> {
    List<ProductionItemHistory> findByProductionItemIdOrderByEnteredAtAsc(Long productionItemId);
    List<ProductionItemHistory> findByStationCode(String stationCode);
}
