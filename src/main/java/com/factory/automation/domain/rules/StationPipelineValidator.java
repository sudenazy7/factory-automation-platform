package com.factory.automation.domain.rules;

import com.factory.automation.domain.enums.ItemStatus;
import com.factory.automation.domain.enums.StationType;
import com.factory.automation.domain.exception.BusinessRuleViolationException;
import com.factory.automation.domain.exception.InvalidStateTransitionException;
import com.factory.automation.domain.exception.StationUnavailableException;
import com.factory.automation.domain.model.ProductionItem;
import com.factory.automation.domain.model.ProductionItemHistory;
import com.factory.automation.domain.model.Station;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StationPipelineValidator {

    public static final int DEFAULT_MAX_REWORK_LIMIT = 2;

    /**
     * Validates if a production item can legitimately enter the target station.
     */
    public void validateStationEntry(ProductionItem item, Station targetStation) {
        if (item.getWorkOrder() != null) {
            item.getWorkOrder().assertNotHalted();
        }

        if (item.getStatus() == ItemStatus.FAILED || item.getStatus() == ItemStatus.PACKED) {
            throw new BusinessRuleViolationException(
                    String.format("Ürün [%s] nihai durumda (%s)! Yeni bir istasyona alınamaz.",
                            item.getSerialNumber(), item.getStatus()));
        }

        if (!targetStation.isAvailable()) {
            throw new StationUnavailableException(targetStation.getStationCode(), targetStation.getStatus().name());
        }

        // Validate station progression sequence
        validateSequenceProgression(item, targetStation);
    }

    private void validateSequenceProgression(ProductionItem item, Station targetStation) {
        List<ProductionItemHistory> histories = item.getStationHistories();

        // First station entry must be RAW_MATERIAL_ACCEPTANCE or ASSEMBLY
        if (histories.isEmpty()) {
            if (targetStation.getStationType() != StationType.RAW_MATERIAL_ACCEPTANCE
                    && targetStation.getStationType() != StationType.ASSEMBLY) {
                throw new InvalidStateTransitionException("ProductionItem",
                        "INITIAL",
                        targetStation.getStationType().name(),
                        "Ürün üretim hattına yalnızca Hammadde Kabul veya Montaj istasyonundan başlayabilir.");
            }
            return;
        }

        // If item is currently under REWORK, it may return to ASSEMBLY or PAINTING
        if (item.getStatus() == ItemStatus.REWORK) {
            if (targetStation.getStationType() != StationType.ASSEMBLY
                    && targetStation.getStationType() != StationType.PAINTING) {
                throw new InvalidStateTransitionException("ProductionItem",
                        "REWORK",
                        targetStation.getStationType().name(),
                        "Rework aşamasındaki ürün yalnızca Montaj veya Boyama istasyonuna dönebilir.");
            }
            return;
        }

        // Pipeline Sequence: Station sequence order must be greater than previously passed station
        ProductionItemHistory lastHistory = histories.get(histories.size() - 1);
        int lastSequence = lastHistory.getStationType().getDefaultSequenceOrder();
        int targetSequence = targetStation.getStationType().getDefaultSequenceOrder();

        if (targetSequence < lastSequence) {
            throw new InvalidStateTransitionException("ProductionItem",
                    lastHistory.getStationType().name(),
                    targetStation.getStationType().name(),
                    "İstasyon sırası geriye doğru işletilemez (Rework haricinde).");
        }

        // Cannot jump directly to PACKAGING without passing QUALITY_CHECK
        if (targetStation.getStationType() == StationType.PACKAGING) {
            boolean hasPassedQualityCheck = histories.stream()
                    .anyMatch(h -> h.getStationType() == StationType.QUALITY_CHECK
                            && h.getStatusAtStation() == ItemStatus.PASSED);

            if (!hasPassedQualityCheck) {
                throw new BusinessRuleViolationException(
                        String.format("Ürün [%s] Kalite Kontrol (QUALITY_CHECK) aşamasından onay almadan Paketleme (PACKAGING) istasyonuna geçemez!",
                                item.getSerialNumber()));
            }
        }
    }
}
