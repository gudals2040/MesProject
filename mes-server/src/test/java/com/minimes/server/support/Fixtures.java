package com.minimes.server.support;

import com.minimes.common.equipment.EquipmentStatus;
import com.minimes.server.equipment.domain.DowntimeCategory;
import com.minimes.server.equipment.domain.DowntimeReason;
import com.minimes.server.equipment.domain.Equipment;
import com.minimes.server.item.domain.Item;
import com.minimes.server.member.domain.Member;
import com.minimes.server.member.domain.MemberRole;
import com.minimes.server.production.domain.DefectType;
import com.minimes.server.workorder.domain.WorkOrder;

import java.time.Duration;
import java.time.Instant;

/**
 * 도메인 단위 테스트용 객체 생성 도우미. DB 없이 엔티티 규칙만 검증한다.
 */
public final class Fixtures {

    public static final Instant T0 = Instant.parse("2026-10-15T00:00:00Z");

    private Fixtures() {
    }

    public static Instant at(long minutes) {
        return T0.plus(Duration.ofMinutes(minutes));
    }

    public static Equipment equipment() {
        return new Equipment("EQ-01", "가류기 1호", T0);
    }

    public static Equipment equipment(EquipmentStatus status) {
        Equipment equipment = equipment();
        equipment.changeStatus(status, T0);
        return equipment;
    }

    /** 관리자 (작업지시 등록·취소, 알람 확인) */
    public static Member admin() {
        return new Member("관리자", MemberRole.ADMIN, T0);
    }

    /** 작업자 (작업 시작·완료, 실적 확정, 비가동 사유 입력) */
    public static Member worker() {
        return new Member("작업자1", MemberRole.WORKER, T0);
    }

    public static Item item() {
        return new Item("TIRE-A", "A 타이어", "EA", T0);
    }

    public static WorkOrder workOrder(Equipment equipment, int plannedQty) {
        return new WorkOrder("WO-001", item(), equipment, plannedQty, admin(), T0);
    }

    /** 설비 RUNNING, 계획 100개, at(0)에 시작한 작업지시 */
    public static WorkOrder startedWorkOrder() {
        WorkOrder workOrder = workOrder(equipment(EquipmentStatus.RUNNING), 100);
        workOrder.start(worker(), at(0));
        return workOrder;
    }

    public static DefectType defectType(String code) {
        return new DefectType(code, code + " 불량");
    }

    public static DowntimeReason noOrderReason() {
        return new DowntimeReason(DowntimeReason.NO_ORDER, "작업 없음", DowntimeCategory.PLANNED);
    }

    public static DowntimeReason breakdownReason() {
        return new DowntimeReason("BREAKDOWN", "설비 고장", DowntimeCategory.UNPLANNED);
    }
}
