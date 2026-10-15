package com.minimes.server.equipment.domain;

import com.minimes.common.equipment.EquipmentStatus;

import java.time.Instant;

/**
 * 설비 상태 전이. 비가동 이력을 어떻게 처리할지 판단하는 데 쓴다.
 * (Notion "상태 전이 · 비즈니스 규칙" > 설비 전이 표)
 */
public record StatusTransition(EquipmentStatus from, EquipmentStatus to, Instant occurredAt) {

    /** RUNNING → IDLE / DOWN : 비가동을 연다 */
    public boolean stopped() {
        return from == EquipmentStatus.RUNNING && to.isStopped();
    }

    /** IDLE / DOWN → RUNNING : 진행 중 비가동을 닫는다 */
    public boolean resumed() {
        return from.isStopped() && to == EquipmentStatus.RUNNING;
    }

    /** IDLE ↔ DOWN : 진행 중 비가동을 닫고 새로 연다 */
    public boolean stopKindChanged() {
        return from.isStopped() && to.isStopped();
    }
}
