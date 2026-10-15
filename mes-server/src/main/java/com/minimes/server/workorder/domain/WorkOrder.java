package com.minimes.server.workorder.domain;

import com.minimes.server.equipment.domain.Equipment;
import com.minimes.server.global.error.ErrorCode;
import com.minimes.server.global.error.MesException;
import com.minimes.server.item.domain.Item;
import com.minimes.server.member.domain.Member;
import com.minimes.server.member.domain.MemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 작업지시 (ADR-008: 작업지시 1건 = 설비 1대).
 * 상태 전이는 이 클래스의 메서드로만 일어난다 (W3).
 *
 * <pre>
 * WAITING ──start──▶ IN_PROGRESS ──complete──▶ COMPLETED
 *    │                    │
 *    └──────cancel────────┴──────cancel──────▶ CANCELED
 * </pre>
 *
 * W1(설비당 진행 중 1건)은 DB 부분 유니크 인덱스가 보장한다.
 * 등록·취소는 관리자, 시작·완료는 작업자만 할 수 있다 (R1, R2).
 * 등록·시작·완료·취소한 사람과 취소 시각을 기록한다 (R3).
 */
@Entity
@Table(name = "work_order")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 30)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "planned_qty", nullable = false)
    private int plannedQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkOrderStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 등록한 관리자 (R3) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Member createdBy;

    /** 시작한 작업자. 시작 전이면 null (R3) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by")
    private Member startedBy;

    /** 완료한 작업자. 완료 전이면 null (R3) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private Member completedBy;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    /** 취소한 관리자. 취소되지 않았으면 null (R3) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canceled_by")
    private Member canceledBy;

    protected WorkOrder() {
    }

    public WorkOrder(String orderNo, Item item, Equipment equipment, int plannedQty,
                     Member admin, Instant now) {
        Member.requireRole(admin, MemberRole.ADMIN);                                  // R1
        if (plannedQty < 1) {
            throw new MesException(ErrorCode.INVALID_PLANNED_QTY);
        }
        this.orderNo = orderNo;
        this.item = item;
        this.equipment = equipment;
        this.plannedQty = plannedQty;
        this.status = WorkOrderStatus.WAITING;
        this.createdAt = now;
        this.createdBy = admin;
    }

    /** WAITING → IN_PROGRESS. 작업자만 시작할 수 있고 (R2), 설비가 DOWN이면 시작할 수 없다 (W2) */
    public void start(Member worker, Instant now) {
        Member.requireRole(worker, MemberRole.WORKER);
        requireStatus(WorkOrderStatus.WAITING);
        if (equipment.isDown()) {
            throw new MesException(ErrorCode.EQUIPMENT_DOWN);
        }
        this.status = WorkOrderStatus.IN_PROGRESS;
        this.startedAt = now;
        this.startedBy = worker;
    }

    /**
     * IN_PROGRESS → COMPLETED.
     * 완료 시각은 마지막으로 확정된 실적 구간의 끝이다 (P6).
     *
     * @param worker                 완료하는 작업자 (R2)
     * @param lastConfirmedPeriodEnd 마지막 확정 실적의 period_end. 확정된 실적이 없으면 null
     */
    public void complete(Member worker, Instant lastConfirmedPeriodEnd) {
        Member.requireRole(worker, MemberRole.WORKER);
        requireStatus(WorkOrderStatus.IN_PROGRESS);
        if (lastConfirmedPeriodEnd == null) {
            throw new MesException(ErrorCode.FINAL_RESULT_NOT_CONFIRMED);
        }
        this.status = WorkOrderStatus.COMPLETED;
        this.completedAt = lastConfirmedPeriodEnd;
        this.completedBy = worker;
    }

    /** WAITING / IN_PROGRESS → CANCELED. 관리자만 취소할 수 있고 (R1), 확정된 실적은 그대로 남는다 */
    public void cancel(Member admin, Instant now) {
        Member.requireRole(admin, MemberRole.ADMIN);
        if (status.isTerminal()) {
            throw new MesException(ErrorCode.INVALID_WORK_ORDER_TRANSITION);
        }
        this.status = WorkOrderStatus.CANCELED;
        this.canceledAt = now;
        this.canceledBy = admin;
    }

    /**
     * 다음 실적 구간의 시작 시각 (P2 구간 연속성).
     * 첫 구간은 작업 시작 시각, 이후는 직전 구간의 끝.
     *
     * @param lastConfirmedPeriodEnd 마지막 확정 실적의 period_end. 없으면 null
     */
    public Instant nextPeriodStart(Instant lastConfirmedPeriodEnd) {
        if (status != WorkOrderStatus.IN_PROGRESS) {
            throw new MesException(ErrorCode.WORK_ORDER_NOT_IN_PROGRESS);
        }
        return lastConfirmedPeriodEnd != null ? lastConfirmedPeriodEnd : startedAt;
    }

    /** 누적 양품이 계획 수량을 넘었는지 (P8: 등록은 허용하고 경고만 표시) */
    public boolean isOverProduced(long cumulativeGoodQty) {
        return cumulativeGoodQty > plannedQty;
    }

    public boolean isInProgress() {
        return status == WorkOrderStatus.IN_PROGRESS;
    }

    private void requireStatus(WorkOrderStatus expected) {
        if (status != expected) {
            throw new MesException(ErrorCode.INVALID_WORK_ORDER_TRANSITION);
        }
    }

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Item getItem() {
        return item;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public int getPlannedQty() {
        return plannedQty;
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Member getCreatedBy() {
        return createdBy;
    }

    public Member getStartedBy() {
        return startedBy;
    }

    public Member getCompletedBy() {
        return completedBy;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public Member getCanceledBy() {
        return canceledBy;
    }
}
