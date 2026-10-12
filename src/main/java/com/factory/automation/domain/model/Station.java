package com.factory.automation.domain.model;

import com.factory.automation.domain.enums.IndustryType;
import com.factory.automation.domain.enums.StationStatus;
import com.factory.automation.domain.enums.StationType;
import com.factory.automation.domain.exception.StationCapacityExceededException;
import com.factory.automation.domain.exception.StationUnavailableException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "İstasyon kodu zorunludur")
    @Column(nullable = false, unique = true, length = 50)
    private String stationCode;

    @NotBlank(message = "İstasyon adı zorunludur")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "İstasyon tipi zorunludur")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StationType stationType;

    @NotNull(message = "İstasyon durumu zorunludur")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StationStatus status = StationStatus.IDLE;

    @Column(nullable = false)
    @Builder.Default
    private int sequenceOrder = 1;

    @PositiveOrZero
    @Column(nullable = false)
    @Builder.Default
    private int currentCapacity = 0;

    @PositiveOrZero
    @Column(nullable = false)
    @Builder.Default
    private int maxCapacity = 5;

    @PositiveOrZero
    @Column(nullable = false)
    @Builder.Default
    private int activeRobotsCount = 0;

    @Column(length = 255)
    private String haltReason;

    @Column
    private Double temperatureThreshold;

    @Column
    private Double currentTemperature;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private IndustryType industryType = IndustryType.GENERAL_MANUFACTURING;

    @Column
    private LocalDateTime lastStatusChange;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        if (lastStatusChange == null) {
            lastStatusChange = LocalDateTime.now();
        }
    }

    public boolean isAvailable() {
        return (status == StationStatus.IDLE || status == StationStatus.ACTIVE)
                && currentCapacity < maxCapacity;
    }

    public void halt(String reason) {
        this.status = StationStatus.HALTED;
        this.haltReason = reason;
        this.lastStatusChange = LocalDateTime.now();
    }

    public void resume() {
        this.status = this.currentCapacity > 0 ? StationStatus.ACTIVE : StationStatus.IDLE;
        this.haltReason = null;
        this.lastStatusChange = LocalDateTime.now();
    }

    public void allocateCapacity() {
        if (this.status == StationStatus.HALTED || this.status == StationStatus.MAINTENANCE || this.status == StationStatus.ERROR) {
            throw new StationUnavailableException(this.stationCode, this.status.name());
        }
        if (this.currentCapacity >= this.maxCapacity) {
            throw new StationCapacityExceededException(this.stationCode, this.maxCapacity);
        }
        this.currentCapacity++;
        this.status = StationStatus.ACTIVE;
    }

    public void releaseCapacity() {
        if (this.currentCapacity > 0) {
            this.currentCapacity--;
        }
        if (this.currentCapacity == 0 && this.status == StationStatus.ACTIVE) {
            this.status = StationStatus.IDLE;
        }
    }
}
