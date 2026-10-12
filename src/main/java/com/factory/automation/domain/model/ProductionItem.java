package com.factory.automation.domain.model;

import com.factory.automation.domain.enums.ItemStatus;
import com.factory.automation.domain.enums.StationType;
import com.factory.automation.domain.exception.BusinessRuleViolationException;
import com.factory.automation.domain.exception.MaxReworkExceededException;
import com.factory.automation.domain.exception.WorkOrderHaltedException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "production_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Seri numarası zorunludur")
    @Column(nullable = false, unique = true, length = 64)
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_station_id")
    private Station currentStation;

    @NotNull(message = "Durum zorunludur")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ItemStatus status = ItemStatus.CREATED;

    @Column(nullable = false)
    @Builder.Default
    private int reworkCount = 0;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "productionItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("enteredAt ASC")
    @Builder.Default
    private List<ProductionItemHistory> stationHistories = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void enterStation(Station station, String operatorOrRobotId) {
        if (this.workOrder != null) {
            this.workOrder.assertNotHalted();
        }
        if (this.status == ItemStatus.FAILED || this.status == ItemStatus.PACKED) {
            throw new BusinessRuleViolationException(
                    String.format("Sonlanmış ürün [%s] yeni istasyona giremez. Mevcut durum: %s",
                            this.serialNumber, this.status));
        }

        // Release old station capacity if currently holding one
        if (this.currentStation != null && this.currentStation != station) {
            this.currentStation.releaseCapacity();
        }

        station.allocateCapacity();
        this.currentStation = station;
        this.status = ItemStatus.IN_PROGRESS;

        ProductionItemHistory history = ProductionItemHistory.builder()
                .productionItem(this)
                .stationCode(station.getStationCode())
                .stationType(station.getStationType())
                .statusAtStation(ItemStatus.IN_PROGRESS)
                .operatorOrRobotId(operatorOrRobotId)
                .enteredAt(LocalDateTime.now())
                .build();

        this.stationHistories.add(history);
    }

    public void passCurrentStation(String operatorOrRobotId, String inspectionResult, String notes) {
        if (this.workOrder != null) {
            this.workOrder.assertNotHalted();
        }
        if (this.currentStation == null) {
            throw new BusinessRuleViolationException("Ürün şu anda hiçbir istasyonda bulunmuyor!");
        }

        closeCurrentHistory(ItemStatus.PASSED, operatorOrRobotId, inspectionResult, notes);
        this.status = ItemStatus.PASSED;

        // If packaging station passed, complete item lifecycle
        if (this.currentStation.getStationType() == StationType.PACKAGING) {
            this.status = ItemStatus.PACKED;
            this.currentStation.releaseCapacity();
            this.currentStation = null;
            if (this.workOrder != null) {
                this.workOrder.recordItemCompletion();
            }
        }
    }

    public void triggerRework(int maxAllowedRework, String operatorOrRobotId, String reason) {
        if (this.workOrder != null) {
            this.workOrder.assertNotHalted();
        }
        this.reworkCount++;
        closeCurrentHistory(ItemStatus.REWORK, operatorOrRobotId, "REWORK_REQUIRED", reason);

        if (this.reworkCount > maxAllowedRework) {
            failItem(String.format("Maksimum rework limiti aşıldı (%d/%d): %s",
                    this.reworkCount, maxAllowedRework, reason));
            throw new MaxReworkExceededException(this.serialNumber, this.reworkCount, maxAllowedRework);
        }

        this.status = ItemStatus.REWORK;
    }

    public void failItem(String reason) {
        closeCurrentHistory(ItemStatus.FAILED, null, "FAILED", reason);
        this.status = ItemStatus.FAILED;
        this.failureReason = reason;

        if (this.currentStation != null) {
            this.currentStation.releaseCapacity();
            this.currentStation = null;
        }

        if (this.workOrder != null) {
            this.workOrder.recordItemFailure();
        }
    }

    private void closeCurrentHistory(ItemStatus finalStatus, String operatorOrRobotId, String inspectionResult, String notes) {
        if (!stationHistories.isEmpty()) {
            ProductionItemHistory latest = stationHistories.get(stationHistories.size() - 1);
            if (latest.getExitedAt() == null) {
                latest.completeStep(finalStatus, operatorOrRobotId, inspectionResult, notes);
            }
        }
    }
}
