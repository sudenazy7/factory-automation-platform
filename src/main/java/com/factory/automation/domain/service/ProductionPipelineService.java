package com.factory.automation.domain.service;

import com.factory.automation.domain.enums.ItemStatus;
import com.factory.automation.domain.enums.StationStatus;
import com.factory.automation.domain.enums.StationType;
import com.factory.automation.domain.enums.WorkOrderStatus;
import com.factory.automation.domain.model.ProductionItem;
import com.factory.automation.domain.model.Station;
import com.factory.automation.domain.model.WorkOrder;
import com.factory.automation.domain.rules.StationPipelineValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductionPipelineService {

    private final StationPipelineValidator pipelineValidator;

    /**
     * Moves a production item into a station, validating sequence and station health.
     */
    public void moveItemToStation(ProductionItem item, Station station, String operatorOrRobotId) {
        pipelineValidator.validateStationEntry(item, station);
        item.enterStation(station, operatorOrRobotId);

        // Update work order status depending on current milestone
        WorkOrder workOrder = item.getWorkOrder();
        if (workOrder != null) {
            if (station.getStationType() == StationType.ASSEMBLY && workOrder.getStatus() == WorkOrderStatus.CREATED) {
                workOrder.startAssembly();
            } else if (station.getStationType() == StationType.QUALITY_CHECK && workOrder.getStatus() == WorkOrderStatus.IN_ASSEMBLY) {
                workOrder.moveToQualityCheck();
            }
        }
    }

    /**
     * Completes work at current station with PASS.
     */
    public void passStation(ProductionItem item, String operatorOrRobotId, String inspectionNotes) {
        item.passCurrentStation(operatorOrRobotId, "PASSED", inspectionNotes);
    }

    /**
     * Handles quality inspection rejection: either routes to rework or fails product.
     */
    public void handleQualityInspectionFailure(ProductionItem item, String operatorOrRobotId, String defectReason, boolean canRework) {
        if (canRework) {
            item.triggerRework(StationPipelineValidator.DEFAULT_MAX_REWORK_LIMIT, operatorOrRobotId, defectReason);
        } else {
            item.failItem(defectReason);
        }
    }

    /**
     * Emergency halt a station (e.g. high telemetry threshold / sensor alarm).
     */
    public void haltStation(Station station, String reason) {
        station.halt(reason);
    }

    /**
     * Resumes a halted station after maintenance/inspection.
     */
    public void resumeStation(Station station) {
        station.resume();
    }
}
