package com.minimes.server.production.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 불량 내역. 생산 실적과 함께만 만들어진다 (ProductionResult.confirm).
 */
@Entity
@Table(name = "defect")
public class Defect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_result_id", nullable = false)
    private ProductionResult productionResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "defect_type_id", nullable = false)
    private DefectType defectType;

    @Column(name = "qty", nullable = false)
    private int qty;

    protected Defect() {
    }

    Defect(ProductionResult productionResult, DefectType defectType, int qty) {
        this.productionResult = productionResult;
        this.defectType = defectType;
        this.qty = qty;
    }

    public Long getId() {
        return id;
    }

    public ProductionResult getProductionResult() {
        return productionResult;
    }

    public DefectType getDefectType() {
        return defectType;
    }

    public int getQty() {
        return qty;
    }
}
