package com.minimes.server.equipment.domain;

import com.minimes.common.equipment.EquipmentStatus;
import com.minimes.server.global.error.ErrorCode;
import com.minimes.server.global.error.MesException;
import com.minimes.server.member.domain.Member;
import com.minimes.server.member.domain.MemberRole;
import com.minimes.server.workorder.domain.WorkOrder;
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
 * 비가동 이력 (ADR-005).
 * 설비가 RUNNING이 아닌 모든 구간을 기록하고 사유로 분류한다.
 *   - 작업 없이 IDLE → 사유 "작업 없음(NO_ORDER)" 자동 분류
 *   - 작업 없이 DOWN → 고장일 수 있으므로 미분류로 열고, 관리자나 작업자가 입력 (T4)
 *   - 작업 중 정지   → 작업지시 연결, 미분류로 열고 작업자가 입력 (R2)
 * 사유를 입력한 사람을 기록한다 (R3). 자동 분류는 처리자 없음.
 *
 * T1(설비당 진행 중 1건)은 DB 부분 유니크 인덱스가 보장한다.
 */
@Entity
@Table(name = "downtime")
public class Downtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    /** null = 작업 없음 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    /** null = 미분류 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reason_id")
    private DowntimeReason reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private DowntimeSource source;

    /** 정지 당시 설비 상태 (IDLE / DOWN) */
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_status", nullable = false, length = 20)
    private EquipmentStatus equipmentStatus;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /** null = 진행 중 */
    @Column(name = "ended_at")
    private Instant endedAt;

    /** 사유를 입력한 작업자. null = 미분류 또는 자동 분류 (R3) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classified_by")
    private Member classifiedBy;

    protected Downtime() {
    }

    private Downtime(Equipment equipment, WorkOrder workOrder, DowntimeReason reason,
                     EquipmentStatus equipmentStatus, Instant startedAt) {
        if (equipmentStatus == null || !equipmentStatus.isStopped()) {
            throw new MesException(ErrorCode.INVALID_DOWNTIME_STATUS);
        }
        this.equipment = equipment;
        this.workOrder = workOrder;
        this.reason = reason;
        this.source = DowntimeSource.AUTO;
        this.equipmentStatus = equipmentStatus;
        this.startedAt = startedAt;
    }

    /**
     * 진행 중인 작업지시가 없을 때 (T4).
     * IDLE이면 "작업 없음"으로 자동 분류하고, DOWN이면 고장 등 이유가 있을 수 있어 미분류로 연다.
     */
    public static Downtime openWithoutWork(Equipment equipment, EquipmentStatus equipmentStatus,
                                           DowntimeReason noOrderReason, Instant startedAt) {
        DowntimeReason reason = equipmentStatus == EquipmentStatus.DOWN ? null : noOrderReason;
        return new Downtime(equipment, null, reason, equipmentStatus, startedAt);
    }

    /** 작업 중에 멈췄을 때: 작업지시를 연결하고 사유는 미분류로 연다 */
    public static Downtime openDuringWork(Equipment equipment, EquipmentStatus equipmentStatus,
                                          WorkOrder workOrder, Instant startedAt) {
        return new Downtime(equipment, workOrder, null, equipmentStatus, startedAt);
    }

    /** 진행 중 비가동을 닫는다 (T2: 종료 >= 시작) */
    public void close(Instant endedAt) {
        if (this.endedAt != null) {
            throw new MesException(ErrorCode.DOWNTIME_ALREADY_CLOSED);
        }
        if (endedAt.isBefore(startedAt)) {
            throw new MesException(ErrorCode.INVALID_DOWNTIME_PERIOD);
        }
        this.endedAt = endedAt;
    }

    /**
     * 사유를 입력한다. 종료된 비가동에도 나중에 입력할 수 있다.
     * 작업 중 정지는 작업자만 (R2), 작업 없는 정지는 관리자도 입력할 수 있다 (T4).
     */
    public void classify(DowntimeReason reason, Member member) {
        if (member == null) {
            throw new MesException(ErrorCode.MEMBER_REQUIRED);
        }
        boolean allowed = member.hasRole(MemberRole.WORKER)
                || (member.hasRole(MemberRole.ADMIN) && workOrder == null);
        if (!allowed) {
            throw new MesException(ErrorCode.FORBIDDEN_ROLE);                         // R2
        }
        if (reason == null) {
            throw new MesException(ErrorCode.DOWNTIME_REASON_REQUIRED);
        }
        this.reason = reason;
        this.source = DowntimeSource.MANUAL;
        this.classifiedBy = member;                                                   // R3
    }

    public boolean isOpen() {
        return endedAt == null;
    }

    public boolean isUnclassified() {
        return reason == null;
    }

    public Long getId() {
        return id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public DowntimeReason getReason() {
        return reason;
    }

    public DowntimeSource getSource() {
        return source;
    }

    public EquipmentStatus getEquipmentStatus() {
        return equipmentStatus;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Member getClassifiedBy() {
        return classifiedBy;
    }
}
