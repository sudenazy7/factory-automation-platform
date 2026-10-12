package com.factory.automation.domain.model;

import com.factory.automation.domain.enums.ItemStatus;
import com.factory.automation.domain.enums.StationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "production_item_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionItemHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_item_id", nullable = false)
    @JsonIgnore
    private ProductionItem productionItem;

    @Column(nullable = false, length = 50)
    private String stationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StationType stationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemStatus statusAtStation;

    @Column(nullable = false)
    private LocalDateTime enteredAt;

    @Column
    private LocalDateTime exitedAt;

    @Column
    private Long durationSeconds;

    @Column(length = 100)
    private String operatorOrRobotId;

    @Column(length = 500)
    private String inspectionResult;

    @Column(length = 1000)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (enteredAt == null) {
            enteredAt = LocalDateTime.now();
        }
    }

    public void completeStep(ItemStatus finalStatus, String operatorOrRobotId, String inspectionResult, String notes) {
        this.exitedAt = LocalDateTime.now();
        this.statusAtStation = finalStatus;
        this.operatorOrRobotId = operatorOrRobotId;
        this.inspectionResult = inspectionResult;
        this.notes = notes;
        if (this.enteredAt != null) {
            this.durationSeconds = java.time.Duration.between(this.enteredAt, this.exitedAt).getSeconds();
        }
    }
}
