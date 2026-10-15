package com.minimes.server.production.domain;

import com.minimes.server.global.error.ErrorCode;
import com.minimes.server.global.error.MesException;
import com.minimes.server.member.domain.Member;
import com.minimes.server.member.domain.MemberRole;
import com.minimes.server.workorder.domain.WorkOrder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 생산 실적 (ADR-004: 작업지시 1:N, ADR-007: 센서 집계 + 작업자 확정).
 *
 * 작업자가 확정하는 시점에 confirm()으로 만들어지며, 이후 수정하는 메서드는 두지 않는다 (P7, ADR-009).
 * 불량 수량(defectQty)은 불량 내역 합계로 계산하므로 D1(합계 일치)이 항상 성립한다.
 * 작업자만 확정할 수 있고 (R2), 확정한 작업자를 기록한다 (R3).
 */
@Entity
@Table(name = "production_result")
public class ProductionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    /** 구간 내 센서 COUNT 합계. 서버가 계산하며 작업자가 수정할 수 없다 (P4) */
    @Column(name = "counted_qty", nullable = false)
    private int countedQty;

    @Column(name = "good_qty", nullable = false)
    private int goodQty;

    @Column(name = "defect_qty", nullable = false)
    private int defectQty;

    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt;

    /** 확정한 작업자 (R3) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "confirmed_by", nullable = false)
    private Member confirmedBy;

    @OneToMany(mappedBy = "productionResult", cascade = CascadeType.PERSIST)
    private List<Defect> defects = new ArrayList<>();

    protected ProductionResult() {
    }

    /**
     * 실적을 확정한다.
     *
     * @param workOrder              진행 중인 작업지시 (P1)
     * @param lastConfirmedPeriodEnd 직전 확정 실적의 period_end, 첫 구간이면 null (P2)
     * @param periodEnd              이번 구간의 끝
     * @param countedQty             서버가 계산한 구간 내 센서 COUNT 합계 (P4)
     * @param goodQty                작업자가 확정한 양품 수량
     * @param defectLines            작업자가 입력한 불량 내역
     * @param worker                 확정하는 작업자 (R2, R3)
     * @param now                    현재 시각
     */
    public static ProductionResult confirm(WorkOrder workOrder,
                                           Instant lastConfirmedPeriodEnd,
                                           Instant periodEnd,
                                           int countedQty,
                                           int goodQty,
                                           List<DefectLine> defectLines,
                                           Member worker,
                                           Instant now) {
        Member.requireRole(worker, MemberRole.WORKER);                               // R2
        Instant periodStart = workOrder.nextPeriodStart(lastConfirmedPeriodEnd);   // P1, P2

        if (!periodEnd.isAfter(periodStart) || periodEnd.isAfter(now)) {          // P3
            throw new MesException(ErrorCode.INVALID_RESULT_PERIOD);
        }
        if (countedQty < 0 || goodQty < 0) {                                        // P5
            throw new MesException(ErrorCode.INVALID_RESULT_QTY);
        }

        ProductionResult result = new ProductionResult();
        result.workOrder = workOrder;
        result.periodStart = periodStart;
        result.periodEnd = periodEnd;
        result.countedQty = countedQty;
        result.goodQty = goodQty;
        result.confirmedAt = now;
        result.confirmedBy = worker;                                                 // R3
        result.addDefects(defectLines);
        return result;
    }

    private void addDefects(List<DefectLine> lines) {
        Set<String> typeCodes = new HashSet<>();
        int total = 0;
        for (DefectLine line : lines) {
            if (line.qty() < 1) {                                                    // D2
                throw new MesException(ErrorCode.INVALID_DEFECT_QTY);
            }
            if (!typeCodes.add(line.defectType().getCode())) {
                throw new MesException(ErrorCode.DUPLICATE_DEFECT_TYPE);
            }
            defects.add(new Defect(this, line.defectType(), line.qty()));
            total += line.qty();
        }
        this.defectQty = total;                                                      // D1
    }

    /** 확정 수량(양품 + 불량)과 센서 집계 수량의 차이. 대시보드 대조용 */
    public int countDifference() {
        return countedQty - (goodQty + defectQty);
    }

    public Long getId() {
        return id;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public int getCountedQty() {
        return countedQty;
    }

    public int getGoodQty() {
        return goodQty;
    }

    public int getDefectQty() {
        return defectQty;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Member getConfirmedBy() {
        return confirmedBy;
    }

    public List<Defect> getDefects() {
        return Collections.unmodifiableList(defects);
    }
}
