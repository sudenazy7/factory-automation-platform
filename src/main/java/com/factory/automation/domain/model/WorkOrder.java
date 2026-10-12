package com.factory.automation.domain.model;

import com.factory.automation.domain.enums.IndustryType;
import com.factory.automation.domain.enums.WorkOrderPriority;
import com.factory.automation.domain.enums.WorkOrderStatus;
import com.factory.automation.domain.exception.BusinessRuleViolationException;
import com.factory.automation.domain.exception.InvalidStateTransitionException;
import com.factory.automation.domain.exception.WorkOrderHaltedException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "İş emri numarası zorunludur")
    @Column(nullable = false, unique = true, length = 64)
    private String orderNumber;

    @NotBlank(message = "Ürün tipi zorunludur")
    @Column(nullable = false, length = 100)
    private String productType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private IndustryType industryType = IndustryType.GENERAL_MANUFACTURING;

    @Min(value = 1, message = "Hedef adet en az 1 olmalıdır")
    @Column(nullable = false)
    private int targetQuantity;

    @Column(nullable = false)
    @Builder.Default
    private int completedQuantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private int failedQuantity = 0;

    @NotNull(message = "Öncelik seçilmelidir")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private WorkOrderPriority priority = WorkOrderPriority.MEDIUM;

    @NotNull(message = "Durum zorunludur")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private WorkOrderStatus status = WorkOrderStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private WorkOrderStatus previousStatus;

    @Column(length = 500)
    private String haltedReason;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<ProductionItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void addItem(ProductionItem item) {
        if (this.status == WorkOrderStatus.HALTED) {
            throw new WorkOrderHaltedException(this.orderNumber, this.haltedReason);
        }
        if (this.items.size() >= this.targetQuantity) {
            throw new BusinessRuleViolationException(
                    String.format("İş emri hedef adedi (%d) aşılamaz! Mevcut kalem sayısı: %d",
                            this.targetQuantity, this.items.size()));
        }
        items.add(item);
        item.setWorkOrder(this);
    }

    public void startAssembly() {
        assertNotHalted();
        if (this.status != WorkOrderStatus.CREATED) {
            throw new InvalidStateTransitionException("WorkOrder", this.status.name(),
                    WorkOrderStatus.IN_ASSEMBLY.name(), "Yalnızca CREATED durumundaki iş emirleri montaja başlayabilir");
        }
        this.status = WorkOrderStatus.IN_ASSEMBLY;
        this.startedAt = LocalDateTime.now();
    }

    public void moveToQualityCheck() {
        assertNotHalted();
        if (this.status != WorkOrderStatus.IN_ASSEMBLY) {
            throw new InvalidStateTransitionException("WorkOrder", this.status.name(),
                    WorkOrderStatus.QUALITY_CHECK.name(), "Yalnızca IN_ASSEMBLY durumundaki iş emirleri kalite testine geçebilir");
        }
        this.status = WorkOrderStatus.QUALITY_CHECK;
    }

    public void completeOrder() {
        assertNotHalted();
        if (this.completedQuantity + this.failedQuantity < this.targetQuantity) {
            throw new BusinessRuleViolationException(
                    String.format("İş emri henüz tamamlanamaz. Hedef: %d, Tamamlanan: %d, Hatalı: %d",
                            this.targetQuantity, this.completedQuantity, this.failedQuantity));
        }
        this.status = WorkOrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void failOrder(String reason) {
        this.status = WorkOrderStatus.FAILED;
        this.notes = (this.notes != null ? this.notes + " | " : "") + "Başarısızlık Nedeni: " + reason;
        this.completedAt = LocalDateTime.now();
    }

    public void haltOrder(String reason) {
        if (this.status == WorkOrderStatus.COMPLETED || this.status == WorkOrderStatus.FAILED) {
            throw new BusinessRuleViolationException("Tamamlanmış veya iptal edilmiş iş emri durdurulamaz");
        }
        this.previousStatus = this.status;
        this.status = WorkOrderStatus.HALTED;
        this.haltedReason = reason;
    }

    public void resumeOrder() {
        if (this.status != WorkOrderStatus.HALTED) {
            throw new BusinessRuleViolationException("İş emri HALTED durumunda değil!");
        }
        this.status = this.previousStatus != null ? this.previousStatus : WorkOrderStatus.IN_ASSEMBLY;
        this.previousStatus = null;
        this.haltedReason = null;
    }

    public void recordItemCompletion() {
        this.completedQuantity++;
        if (isAllItemsProcessed()) {
            this.status = WorkOrderStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }

    public void recordItemFailure() {
        this.failedQuantity++;
        if (isAllItemsProcessed()) {
            if (this.completedQuantity == 0) {
                this.status = WorkOrderStatus.FAILED;
            } else {
                this.status = WorkOrderStatus.COMPLETED;
            }
            this.completedAt = LocalDateTime.now();
        }
    }

    public boolean isAllItemsProcessed() {
        return (this.completedQuantity + this.failedQuantity) >= this.targetQuantity;
    }

    public void assertNotHalted() {
        if (this.status == WorkOrderStatus.HALTED) {
            throw new WorkOrderHaltedException(this.orderNumber, this.haltedReason);
        }
    }
}
