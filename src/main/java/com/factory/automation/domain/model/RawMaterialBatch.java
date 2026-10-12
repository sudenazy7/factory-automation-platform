package com.factory.automation.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_material_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawMaterialBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Parti numarası zorunludur")
    @Column(nullable = false, unique = true, length = 64)
    private String batchNumber;

    @NotBlank(message = "Malzeme tipi zorunludur")
    @Column(nullable = false, length = 100)
    private String materialType;

    @NotNull(message = "Miktar zorunludur")
    @Positive(message = "Miktar pozitif olmalıdır")
    @Column(nullable = false)
    private Double quantity;

    @NotBlank(message = "Birim zorunludur")
    @Column(nullable = false, length = 20)
    private String unit;

    @NotBlank(message = "Tedarikçi bilgisi zorunludur")
    @Column(nullable = false, length = 150)
    private String supplier;

    @Column(nullable = false)
    private LocalDateTime acceptanceDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean qualityApproved = false;

    @Column(length = 500)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (acceptanceDate == null) {
            acceptanceDate = LocalDateTime.now();
        }
    }
}
