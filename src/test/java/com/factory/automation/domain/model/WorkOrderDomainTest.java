package com.factory.automation.domain.model;

import com.factory.automation.domain.enums.WorkOrderPriority;
import com.factory.automation.domain.enums.WorkOrderStatus;
import com.factory.automation.domain.exception.BusinessRuleViolationException;
import com.factory.automation.domain.exception.InvalidStateTransitionException;
import com.factory.automation.domain.exception.WorkOrderHaltedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkOrderDomainTest {

    private WorkOrder workOrder;

    @BeforeEach
    void setUp() {
        workOrder = WorkOrder.builder()
                .orderNumber("WO-2026-001")
                .productType("Elektrikli Araç Şasisi")
                .targetQuantity(2)
                .priority(WorkOrderPriority.HIGH)
                .status(WorkOrderStatus.CREATED)
                .dueDate(LocalDateTime.now().plusDays(5))
                .build();
    }

    @Test
    @DisplayName("Yeni oluşturulan iş emri varsayılan CREATED durumunda olmalıdır")
    void testInitialWorkOrderState() {
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.CREATED);
        assertThat(workOrder.getCompletedQuantity()).isZero();
        assertThat(workOrder.getFailedQuantity()).isZero();
        assertThat(workOrder.getItems()).isEmpty();
    }

    @Test
    @DisplayName("İş emri hedef adedinden fazla ürün eklenemez")
    void testCannotAddItemsBeyondTargetQuantity() {
        ProductionItem item1 = ProductionItem.builder().serialNumber("SN-001").build();
        ProductionItem item2 = ProductionItem.builder().serialNumber("SN-002").build();
        ProductionItem item3 = ProductionItem.builder().serialNumber("SN-003").build();

        workOrder.addItem(item1);
        workOrder.addItem(item2);

        assertThat(workOrder.getItems()).hasSize(2);

        assertThatThrownBy(() -> workOrder.addItem(item3))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("hedef adedi (2) aşılamaz");
    }

    @Test
    @DisplayName("CREATED durumundan IN_ASSEMBLY durumuna başarıyla geçilmelidir")
    void testStartAssemblyTransition() {
        workOrder.startAssembly();

        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.IN_ASSEMBLY);
        assertThat(workOrder.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("CREATED durumunda olmadan doğrudan QUALITY_CHECK aşamasına geçilemez")
    void testInvalidTransitionToQualityCheck() {
        assertThatThrownBy(() -> workOrder.moveToQualityCheck())
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Yalnızca IN_ASSEMBLY durumundaki iş emirleri kalite testine geçebilir");
    }

    @Test
    @DisplayName("İş emri acil durdurulduğunda (HALTED) işlem yapılamaz ve istisna fırlatılır")
    void testHaltAndResumeWorkflow() {
        workOrder.startAssembly();
        workOrder.haltOrder("Fırın sıcaklığı 210°C kritik seviyede!");

        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.HALTED);
        assertThat(workOrder.getHaltedReason()).contains("210°C");

        assertThatThrownBy(() -> workOrder.moveToQualityCheck())
                .isInstanceOf(WorkOrderHaltedException.class)
                .hasMessageContaining("HALTED (Durduruldu) durumunda");

        // Resume edilince önceki duruma geri dönmelidir
        workOrder.resumeOrder();
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.IN_ASSEMBLY);
        assertThat(workOrder.getHaltedReason()).isNull();
    }

    @Test
    @DisplayName("Tüm kalemler tamamlandığında iş emri COMPLETED durumuna geçmelidir")
    void testAutomaticCompletionWhenAllItemsDone() {
        ProductionItem item1 = ProductionItem.builder().serialNumber("SN-001").build();
        ProductionItem item2 = ProductionItem.builder().serialNumber("SN-002").build();
        workOrder.addItem(item1);
        workOrder.addItem(item2);

        workOrder.recordItemCompletion();
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.CREATED); // Henüz 1 tamamlandı

        workOrder.recordItemCompletion();
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
        assertThat(workOrder.getCompletedAt()).isNotNull();
        assertThat(workOrder.getCompletedQuantity()).isEqualTo(2);
    }
}
