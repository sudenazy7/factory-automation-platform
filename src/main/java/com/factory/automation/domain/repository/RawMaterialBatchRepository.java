package com.factory.automation.domain.repository;

import com.factory.automation.domain.model.RawMaterialBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RawMaterialBatchRepository extends JpaRepository<RawMaterialBatch, Long> {
    Optional<RawMaterialBatch> findByBatchNumber(String batchNumber);
    List<RawMaterialBatch> findByMaterialType(String materialType);
    List<RawMaterialBatch> findByQualityApproved(boolean qualityApproved);
}
