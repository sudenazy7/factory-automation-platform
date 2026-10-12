package com.factory.automation.domain.model;

import com.factory.automation.domain.enums.StationStatus;
import com.factory.automation.domain.enums.StationType;
import com.factory.automation.domain.exception.StationCapacityExceededException;
import com.factory.automation.domain.exception.StationUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StationDomainTest {

    private Station assemblyStation;

    @BeforeEach
    void setUp() {
        assemblyStation = Station.builder()
                .stationCode("STA-ASM-01")
                .name("Montaj Robot İstasyonu")
                .stationType(StationType.ASSEMBLY)
                .status(StationStatus.IDLE)
                .maxCapacity(2)
                .currentCapacity(0)
                .activeRobotsCount(4)
                .build();
    }

    @Test
    @DisplayName("Boş istasyona kapasite tahsis edildiğinde ACTIVE durumuna geçmelidir")
    void testAllocateCapacity() {
        assertThat(assemblyStation.isAvailable()).isTrue();

        assemblyStation.allocateCapacity();
        assertThat(assemblyStation.getCurrentCapacity()).isEqualTo(1);
        assertThat(assemblyStation.getStatus()).isEqualTo(StationStatus.ACTIVE);

        assemblyStation.allocateCapacity();
        assertThat(assemblyStation.getCurrentCapacity()).isEqualTo(2);
        assertThat(assemblyStation.isAvailable()).isFalse(); // Kapasite doldu
    }

    @Test
    @DisplayName("Kapasite aşıldığında StationCapacityExceededException fırlatılmalıdır")
    void testExceedCapacityThrowsException() {
        assemblyStation.allocateCapacity();
        assemblyStation.allocateCapacity();

        assertThatThrownBy(() -> assemblyStation.allocateCapacity())
                .isInstanceOf(StationCapacityExceededException.class)
                .hasMessageContaining("maksimum kapasitesine (2) ulaştı");
    }

    @Test
    @DisplayName("Tüm kapasite serbest bırakıldığında istasyon IDLE durumuna dönmelidir")
    void testReleaseCapacity() {
        assemblyStation.allocateCapacity();
        assertThat(assemblyStation.getStatus()).isEqualTo(StationStatus.ACTIVE);

        assemblyStation.releaseCapacity();
        assertThat(assemblyStation.getCurrentCapacity()).isZero();
        assertThat(assemblyStation.getStatus()).isEqualTo(StationStatus.IDLE);
    }

    @Test
    @DisplayName("İstasyon durdurulduğunda (HALTED) yeni kapasite ayrılamaz")
    void testHaltedStationCannotAllocate() {
        assemblyStation.halt("Sensör veri akışı kesildi");

        assertThat(assemblyStation.getStatus()).isEqualTo(StationStatus.HALTED);
        assertThat(assemblyStation.isAvailable()).isFalse();

        assertThatThrownBy(() -> assemblyStation.allocateCapacity())
                .isInstanceOf(StationUnavailableException.class)
                .hasMessageContaining("işlem için uygun değil");

        // Bakım sonrası devam etme
        assemblyStation.resume();
        assertThat(assemblyStation.getStatus()).isEqualTo(StationStatus.IDLE);
        assertThat(assemblyStation.isAvailable()).isTrue();
    }
}
