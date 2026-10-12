package com.factory.automation.domain.model;

import com.factory.automation.domain.enums.ItemStatus;
import com.factory.automation.domain.enums.StationStatus;
import com.factory.automation.domain.enums.StationType;
import com.factory.automation.domain.enums.WorkOrderStatus;
import com.factory.automation.domain.exception.BusinessRuleViolationException;
import com.factory.automation.domain.exception.InvalidStateTransitionException;
import com.factory.automation.domain.exception.MaxReworkExceededException;
import com.factory.automation.domain.exception.WorkOrderHaltedException;
import com.factory.automation.domain.rules.StationPipelineValidator;
import com.factory.automation.domain.service.ProductionPipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionItemPipelineTest {

    private ProductionPipelineService pipelineService;
    private Station rawMaterialStation;
    private Station assemblyStation;
    private Station paintingStation;
    private Station qualityCheckStation;
    private Station packagingStation;
    private WorkOrder workOrder;
    private ProductionItem item;

    @BeforeEach
    void setUp() {
        StationPipelineValidator validator = new StationPipelineValidator();
        pipelineService = new ProductionPipelineService(validator);

        rawMaterialStation = Station.builder()
                .stationCode("STA-RAW-01")
                .name("Hammadde Giriş")
                .stationType(StationType.RAW_MATERIAL_ACCEPTANCE)
                .status(StationStatus.IDLE)
                .maxCapacity(5)
                .sequenceOrder(1)
                .build();

        assemblyStation = Station.builder()
                .stationCode("STA-ASM-01")
                .name("Kaynak & Montaj")
                .stationType(StationType.ASSEMBLY)
                .status(StationStatus.IDLE)
                .maxCapacity(5)
                .sequenceOrder(2)
                .activeRobotsCount(2)
                .build();

        paintingStation = Station.builder()
                .stationCode("STA-PNT-01")
                .name("Boya & Fırın")
                .stationType(StationType.PAINTING)
                .status(StationStatus.IDLE)
                .maxCapacity(5)
                .sequenceOrder(3)
                .temperatureThreshold(200.0)
                .build();

        qualityCheckStation = Station.builder()
                .stationCode("STA-QC-01")
                .name("Kalite Test İstasyonu")
                .stationType(StationType.QUALITY_CHECK)
                .status(StationStatus.IDLE)
                .maxCapacity(5)
                .sequenceOrder(4)
                .build();

        packagingStation = Station.builder()
                .stationCode("STA-PKG-01")
                .name("Paketleme & Sevkiyat")
                .stationType(StationType.PACKAGING)
                .status(StationStatus.IDLE)
                .maxCapacity(5)
                .sequenceOrder(5)
                .build();

        workOrder = WorkOrder.builder()
                .orderNumber("WO-2026-TEST")
                .productType("Otonom Taşıt Şasisi")
                .targetQuantity(1)
                .status(WorkOrderStatus.CREATED)
                .dueDate(LocalDateTime.now().plusDays(2))
                .build();

        item = ProductionItem.builder()
                .serialNumber("SN-TEST-0001")
                .status(ItemStatus.CREATED)
                .build();

        workOrder.addItem(item);
    }

    @Test
    @DisplayName("Ürün adım adım tüm istasyonlardan başarıyla geçmeli ve PACKED durumuna ulaşmalıdır")
    void testHappyPathFullPipelineProgression() {
        // 1. Hammadde Kabul
        pipelineService.moveItemToStation(item, rawMaterialStation, "OP-RECEIVE");
        assertThat(item.getStatus()).isEqualTo(ItemStatus.IN_PROGRESS);
        assertThat(rawMaterialStation.getCurrentCapacity()).isEqualTo(1);
        pipelineService.passStation(item, "OP-RECEIVE", "Malzeme specs uygun");
        assertThat(item.getStatus()).isEqualTo(ItemStatus.PASSED);

        // 2. Montaj
        pipelineService.moveItemToStation(item, assemblyStation, "ROBOT-ARM-01");
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.IN_ASSEMBLY);
        pipelineService.passStation(item, "ROBOT-ARM-01", "Gövde birleştirildi");

        // 3. Boyama
        pipelineService.moveItemToStation(item, paintingStation, "FURNACE-ROBOT-02");
        pipelineService.passStation(item, "FURNACE-ROBOT-02", "Boya kalınlığı 120 mikron");

        // 4. Kalite Testi
        pipelineService.moveItemToStation(item, qualityCheckStation, "QC-OPERATOR-AYSE");
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.QUALITY_CHECK);
        pipelineService.passStation(item, "QC-OPERATOR-AYSE", "Tüm toleranslar karşılandı");

        // 5. Paketleme
        pipelineService.moveItemToStation(item, packagingStation, "PKG-ROBOT-01");
        pipelineService.passStation(item, "PKG-ROBOT-01", "Kutu mühürlendi");

        // Nihai Durum Kontrolleri
        assertThat(item.getStatus()).isEqualTo(ItemStatus.PACKED);
        assertThat(item.getCurrentStation()).isNull(); // Çıkış yaptı
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.COMPLETED); // Hedef adet 1 idi, tamamlandı
        assertThat(workOrder.getCompletedQuantity()).isEqualTo(1);
        assertThat(packagingStation.getCurrentCapacity()).isZero();
    }

    @Test
    @DisplayName("Kalite testinden geçmeden doğrudan Paketleme istasyonuna atlanamaz")
    void testCannotSkipQualityCheckToPackaging() {
        pipelineService.moveItemToStation(item, assemblyStation, "ROBOT-ARM");
        pipelineService.passStation(item, "ROBOT-ARM", "Montaj tamam");

        // Montajdan doğrudan paketlemeye geçmeye çalış
        assertThatThrownBy(() -> pipelineService.moveItemToStation(item, packagingStation, "PKG-OP"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Kalite Kontrol (QUALITY_CHECK) aşamasından onay almadan Paketleme");
    }

    @Test
    @DisplayName("İş emri HALTED durumundayken ürün istasyon değiştiremez")
    void testCannotProgressWhenWorkOrderIsHalted() {
        workOrder.haltOrder("Acil durum butonu basıldı");

        assertThatThrownBy(() -> pipelineService.moveItemToStation(item, assemblyStation, "ROBOT-ARM"))
                .isInstanceOf(WorkOrderHaltedException.class)
                .hasMessageContaining("HALTED (Durduruldu) durumunda");
    }

    @Test
    @DisplayName("Kalite kontrolünde başarısız olan ürün Rework alabilmeli, limit aşılınca FAILED olmalıdır")
    void testReworkCycleAndMaxReworkEnforcement() {
        // Montaj ve Kalite Testine giriş
        pipelineService.moveItemToStation(item, assemblyStation, "ROBOT-ARM");
        pipelineService.passStation(item, "ROBOT-ARM", "Montaj yapıldı");

        pipelineService.moveItemToStation(item, qualityCheckStation, "QC-OP");

        // 1. Defa Rework verilmesi
        pipelineService.handleQualityInspectionFailure(item, "QC-OP", "Kaynak birleşim hatası", true);
        assertThat(item.getStatus()).isEqualTo(ItemStatus.REWORK);
        assertThat(item.getReworkCount()).isEqualTo(1);

        // Rework alan ürün tekrar montaja döner
        pipelineService.moveItemToStation(item, assemblyStation, "ROBOT-REWORK");
        pipelineService.passStation(item, "ROBOT-REWORK", "Yeniden kaynak yapıldı");

        pipelineService.moveItemToStation(item, qualityCheckStation, "QC-OP");

        // 2. Defa Rework verilmesi
        pipelineService.handleQualityInspectionFailure(item, "QC-OP", "İkinci test başarısız", true);
        assertThat(item.getReworkCount()).isEqualTo(2);

        pipelineService.moveItemToStation(item, assemblyStation, "ROBOT-REWORK");
        pipelineService.passStation(item, "ROBOT-REWORK", "Son düzeltme");

        pipelineService.moveItemToStation(item, qualityCheckStation, "QC-OP");

        // 3. Defa Rework denemesi (Limit = 2) -> MaxReworkExceededException ve FAILED
        assertThatThrownBy(() -> pipelineService.handleQualityInspectionFailure(item, "QC-OP", "Üçüncü test de başarısız", true))
                .isInstanceOf(MaxReworkExceededException.class)
                .hasMessageContaining("maksimum yeniden işlem (rework) sınırı aşıldı");

        assertThat(item.getStatus()).isEqualTo(ItemStatus.FAILED);
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.FAILED);
        assertThat(workOrder.getFailedQuantity()).isEqualTo(1);
    }
}
